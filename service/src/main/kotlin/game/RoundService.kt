package org.example.game

import jakarta.inject.Named
import org.example.Either
import org.example.GameEvent
import org.example.GameEventType
import org.example.GameNotificationService
import org.example.TransactionManager
import org.example.entity.game.RoundInfo
import org.example.entity.game.RoundWinnerInfo
import org.example.failure
import org.example.onFailure
import org.example.success
import java.sql.Time

@Named
class RoundService(
    private val trxManager: TransactionManager,
    private val validationService: GameValidationService,
    private val notificationService: GameNotificationService,
) {
    fun startRound(gameId: Int): Either<GameError, Int> =
        trxManager.run {
            validationService
                .run {
                    validateBasicGameAccess(gameId)
                }.onFailure { return@run failure(it) }

            // Check if there are players
            val players = repositoryGame.listPlayersInGame(gameId)
            if (players.listPlayersInGame.isEmpty()) {
                return@run failure(GameError.NoPlayersInGame)
            }

            // Only check previous round completion if there are rounds
            if (repositoryGame.hasActiveRound(gameId)) {
                val scoreboard = repositoryGame.getScores(gameId)
                val finishedPlayers = scoreboard.pointsQueue.size
                val totalPlayers = players.listPlayersInGame.size

                if (finishedPlayers < totalPlayers) {
                    return@run failure(GameError.AllPlayersNotFinished)
                }
            }

            val roundNumber = repositoryGame.startRound(gameId)
            val maxRounds = repositoryGame.getTotalRoundsOfGame(gameId)
            val roundOrder = repositoryGame.getRoundOrder(gameId)
            if (roundNumber < maxRounds) {
                repositoryGame.populateEmptyTurns(matchId = gameId, roundNumber = roundNumber)
            }

            val scoreboard = repositoryGame.getScores(gameId)
            notificationService.notifyGame(
                gameId,
                GameEvent(
                    type = GameEventType.ROUND_STARTED,
                    gameId = gameId,
                    message = "Round $roundNumber has started",
                    data =
                        mapOf(
                            "roundNumber" to roundNumber,
                            "roundOrder" to roundOrder,
                            "nextPlayer" to roundOrder[0],
                            "players" to
                                scoreboard.pointsQueue.map { pointPlayer ->
                                    mapOf(
                                        "playerId" to pointPlayer.player.playerId,
                                        "username" to pointPlayer.player.name.value,
                                        "points" to pointPlayer.points.points,
                                    )
                                },
                        ),
                ),
            )

            success(roundNumber)
        }

    fun getRoundWinner(gameId: Int): Either<GameError, RoundWinnerInfo> =
        trxManager.run {
            validationService
                .run {
                    validateBasicGameAccess(gameId)
                }.onFailure { return@run failure(it) }

            validationService.run { validateRoundInProgress(gameId) }.onFailure { return@run failure(it) }

            // Check if all players finished their turns
            val players = repositoryGame.listPlayersInGame(gameId)
            val scoreboard = repositoryGame.getScores(gameId)

            if (scoreboard.pointsQueue.size < players.listPlayersInGame.size) {
                return@run failure(GameError.AllPlayersNotFinished)
            }

            val winner = repositoryGame.getRoundWinner(gameId)

            val roundNumber = repositoryGame.getCurrentRoundNumber(gameId) ?: failure(GameError.RoundNotStarted)
            val maxRoundNumber = repositoryGame.getTotalRoundsOfGame(gameId)

            notificationService.notifyGame(
                gameId,
                GameEvent(
                    type = GameEventType.ROUND_ENDED,
                    gameId = gameId,
                    message = "Round $roundNumber ended! Winner: ${winner.player.name.value}",
                    data =
                        mapOf(
                            "roundNumber" to roundNumber,
                            "winner" to
                                mapOf(
                                    "playerId" to winner.player.playerId,
                                    "username" to winner.player.name.value,
                                    "points" to winner.points.points,
                                    "handValue" to winner.handValue.name,
                                ),
                            "totalRounds" to maxRoundNumber,
                        ),
                ),
            )

            if (roundNumber == maxRoundNumber) {
                repositoryGame.closeGame(gameId)
                val gameWinner = repositoryGame.getGameWinner(gameId)

                notificationService.notifyGame(
                    gameId,
                    GameEvent(
                        type = GameEventType.GAME_ENDED,
                        gameId = gameId,
                        message = "Game has ended! Final winner: ${gameWinner.player.name.value}",
                        data =
                            mapOf(
                                "winner" to
                                    mapOf(
                                        "playerId" to gameWinner.player.playerId,
                                        "username" to gameWinner.player.name.value,
                                        "totalPoints" to gameWinner.totalPoints.points,
                                        "roundsWon" to gameWinner.roundsWon,
                                    ),
                                "finalRound" to roundNumber,
                            ),
                    ),
                )
            }

            success(winner)
        }

    fun getRoundInfo(gameId: Int): Either<GameError, RoundInfo> =
        trxManager.run {
            validationService
                .run {
                    validateBasicGameAccess(gameId)
                }.onFailure { return@run failure(it) }

            val roundInfo = repositoryGame.getRoundInfo(gameId)
            success(roundInfo)
        }

    fun remainingTime(gameId: Int): Either<GameError, Time> =
        trxManager.run {
            validationService.validateGameId(gameId).onFailure { return@run failure(it) }
            validationService.run { checkGameExists(gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateRoundInProgress(gameId) }.onFailure { return@run failure(it) }

            val time = repositoryGame.remainingTime(gameId)
            success(time)
        }
}
