package org.example.game

import jakarta.inject.Named
import org.example.Either
import org.example.GameEvent
import org.example.GameEventType
import org.example.GameNotificationService
import org.example.TransactionManager
import org.example.entity.game.GameWinnerInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.failure
import org.example.onFailure
import org.example.success

@Named
class GameEndService(
    private val trxManager: TransactionManager,
    private val notificationService: GameNotificationService,
    private val validationService: GameValidationService,
) {
    fun createGame(
        userId: Int,
        lobbyId: Int,
    ): Either<GameError, CreatedGame> =
        trxManager.run {
            validationService.validateUserId(userId).onFailure { return@run failure(it) }
            if (lobbyId <= 0) return@run failure(GameError.LobbyNotFound)

            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(GameError.LobbyNotFound)

            val lobbyPlayers = repositoryLobby.findById(lobbyId)?.currentPlayers ?: return@run failure(GameError.LobbyNotFound)

            if (!repositoryLobby.isUserInLobby(userId, lobbyId)) {
                return@run failure(GameError.UserNotInGame)
            }

            val gameId = repositoryGame.insertMatch(lobby.rounds)

            lobbyPlayers.forEachIndexed { index, player ->
                repositoryGame.insertMatchPlayer(gameId, player.id, index + 1)
            }

            val firstRoundNumber = repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, firstRoundNumber)

            val playerRandomOrder = lobby.currentPlayers.shuffled().map { it.id }
            playerRandomOrder.forEachIndexed { index, userId ->
                repositoryGame.insertRoundOrder(gameId, firstRoundNumber, index + 1, userId)
            }

            success(CreatedGame(gameId))
        }

    fun closeGame(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> =
        trxManager.run {
            validationService.run { validatePlayerGameAccess(userId, gameId) }.onFailure { return@run failure(it) }

            notificationService.notifyGame(
                gameId,
                GameEvent(
                    type = GameEventType.GAME_ENDED,
                    gameId = gameId,
                    message = "Game has ended!",
                    data = emptyMap(),
                ),
            )

            repositoryGame.closeGame(gameId)
            notificationService.closeGameConnections(gameId)
            success(Unit)
        }

    fun listPlayersInGame(gameId: Int): Either<GameError, ListPlayersInGame> =
        trxManager.run {
            validationService.validateGameId(gameId).onFailure { return@run failure(it) }
            validationService.run { checkGameExists(gameId) }.onFailure { return@run failure(it) }

            val players = repositoryGame.listPlayersInGame(gameId)

            if (players.listPlayersInGame.isEmpty()) {
                return@run failure(GameError.NoPlayersInGame)
            }

            success(players)
        }

    fun getGameWinner(gameId: Int): Either<GameError, GameWinnerInfo> =
        trxManager.run {
            validationService.run { validateBasicGameAccess(gameId) }.onFailure { return@run failure(it) }

            val roundInfo = repositoryGame.getRoundInfo(gameId)

            if (roundInfo.round.round <= 0) {
                return@run failure(GameError.NoRoundInProgress)
            }

            val scoreboard = repositoryGame.getScores(gameId)
            if (scoreboard.pointsQueue.isEmpty()) {
                return@run failure(GameError.GameNotFinished)
            }

            val players = repositoryGame.listPlayersInGame(gameId)
            if (scoreboard.pointsQueue.size < players.listPlayersInGame.size) {
                return@run failure(GameError.AllPlayersNotFinished)
            }

            val winnerInfo =
                repositoryGame.findGameWinner(gameId)
                    ?: return@run failure(GameError.GameNotFinished)

            val winnerId = winnerInfo.player.playerId
            repositoryGame.setGameWinnerAndFinish(gameId, winnerId)
            val finalScores = repositoryGame.getFinalScoresRaw(gameId, winnerId)
            finalScores.forEach { (userId, totalScore, isWinner) ->
                if (isWinner) {
                    repositoryGame.updateStatsGameWinner(userId, totalScore)
                } else {
                    repositoryGame.updateStatsGameLoser(userId, totalScore)
                }
            }

            success(winnerInfo)
        }

    fun getScores(gameId: Int): Either<GameError, Scoreboard> =
        trxManager.run {
            validationService.run { validateBasicGameAccess(gameId) }.onFailure { return@run failure(it) }

            val scoreboard = repositoryGame.getScores(gameId)
            success(scoreboard)
        }

    fun leaveGame(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> =
        trxManager.run {
            validationService.run { validateBasicGameAccess(gameId) }.onFailure { return@run failure(it) }
            val players = repositoryGame.listPlayersInGame(gameId)
            val activePlayers = players.listPlayersInGame.toMutableList()

            val leavingPlayer =
                activePlayers.find { it.playerId == userId }
                    ?: return@run failure(GameError.UserNotInGame)

            repositoryGame.removePlayerFromGame(gameId, userId)
            activePlayers.removeIf { it.playerId == userId }

            notificationService.notifyGame(
                gameId,
                GameEvent(
                    type = GameEventType.PLAYER_LEAVE,
                    gameId = gameId,
                    message = "Player ${leavingPlayer.name} left the game",
                    data =
                        mapOf(
                            "removedPlayerId" to userId,
                            "players" to
                                activePlayers.map {
                                    mapOf("id" to it.playerId, "name" to it.name)
                                },
                        ),
                ),
            )

            if (activePlayers.size == 1) {
                val winner = activePlayers.first()

                repositoryGame.setGameWinnerAndFinish(gameId, winner.playerId)
                val finalScores = repositoryGame.getFinalScoresRaw(gameId, winner.playerId)

                finalScores.forEach { (pId, score, isWinner) ->
                    if (isWinner) {
                        repositoryGame.updateStatsGameWinner(pId, score)
                    } else {
                        repositoryGame.updateStatsGameLoser(pId, score)
                    }
                }

                val winnerScore = finalScores.find { it.first == winner.playerId }?.second ?: 0
                val round = repositoryGame.getRoundInfo(gameId)

                notificationService.notifyGame(
                    gameId,
                    GameEvent(
                        type = GameEventType.GAME_ENDED,
                        gameId = gameId,
                        message = "Game has ended! Final winner: ${winner.name}",
                        data =
                            mapOf(
                                "winner" to
                                    mapOf(
                                        "playerId" to winner.playerId,
                                        "username" to winner.name,
                                        "totalPoints" to winnerScore,
                                        "roundsWon" to round.totalRounds,
                                    ),
                                "finalRound" to (round.round.round),
                            ),
                    ),
                )
            }
            success(Unit)
        }
}
