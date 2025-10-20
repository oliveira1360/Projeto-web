package org.example

import jakarta.inject.Named
import org.example.config.GameDomainConfig
import org.example.entity.core.Points
import org.example.entity.dice.createRandomDice
import org.example.entity.game.GameWinnerInfo
import org.example.entity.game.RoundInfo
import org.example.entity.game.RoundWinnerInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import java.sql.Time

sealed class GameError {
    data object EmptyHand : GameError()

    data object NoRoundInProgress : GameError()

    data object GameNotFinished : GameError()

    data object GameNotFound : GameError()

    data object LobbyNotFound : GameError()

    data object InvalidGameId : GameError()

    data object InvalidUserId : GameError()

    data object GameAlreadyFinished : GameError()

    data object NoPlayersInGame : GameError()

    data object InvalidDiceIndices : GameError()

    data object TooManyRolls : GameError()

    data object RoundNotStarted : GameError()

    data object AllPlayersNotFinished : GameError()

    data object UserNotInGame : GameError()

    data object UnauthorizedAction : GameError()

    data object NotPlayerTurn : GameError()
}

data class CreatedGame(
    val gameId: Int,
    val status: String = "ACTIVE",
)

@Named
class GameService(
    private val trxManager: TransactionManager,
    private val config: GameDomainConfig,
) {
    /**
     * Validates that the user is a player in the specified game.
     */
    private fun Transaction.validateUserInGame(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> {
        val players = repositoryGame.listPlayersInGame(gameId)
        val isPlayerInGame = players.listPlayersInGame.any { it.playerId == userId }

        return if (isPlayerInGame) {
            success(Unit)
        } else {
            failure(GameError.UserNotInGame)
        }
    }

    /**
     * Validates that a round is currently in progress.
     */
    private fun Transaction.validateRoundInProgress(gameId: Int): Either<GameError, RoundInfo> {
        val roundInfo = repositoryGame.getRoundInfo(gameId)

        return if (roundInfo.round.round <= 0) {
            failure(GameError.RoundNotStarted)
        } else {
            success(roundInfo)
        }
    }

    /**
     * Validates that it's the player's turn (if applicable).
     */
    private fun Transaction.validatePlayerTurn(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> {
        // TODO
        return success(Unit)
    }

    fun createGame(
        userId: Int,
        lobbyId: Int,
    ): Either<GameError, CreatedGame> =
        trxManager.run {
            // Validate IDs
            if (userId <= 0) return@run failure(GameError.InvalidUserId)
            if (lobbyId <= 0) return@run failure(GameError.LobbyNotFound)

            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(GameError.LobbyNotFound)

            if (!repositoryLobby.isUserInLobby(userId, lobbyId)) {
                return@run failure(GameError.UserNotInGame)
            }

            val gameId = repositoryGame.createGame(userId, lobbyId)
            success(CreatedGame(gameId))
        }

    fun closeGame(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> =
        trxManager.run {
            // Validate IDs
            if (userId <= 0) return@run failure(GameError.InvalidUserId)
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            // Verify user is in the game
            validateUserInGame(userId, gameId).onFailure { return@run failure(it) }

            repositoryGame.closeGame(userId, gameId)
            success(Unit)
        }

    fun listPlayersInGame(gameId: Int): Either<GameError, ListPlayersInGame> =
        trxManager.run {
            // Validate game ID
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            val players = repositoryGame.listPlayersInGame(gameId)

            if (players.listPlayersInGame.isEmpty()) {
                return@run failure(GameError.NoPlayersInGame)
            }

            success(players)
        }

    fun startRound(gameId: Int): Either<GameError, Int> =
        trxManager.run {
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

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
            success(roundNumber)
        }

    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Hand> =
        trxManager.run {
            // Validate IDs
            if (userId <= 0) return@run failure(GameError.InvalidUserId)
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            // Verify user is in the game
            validateUserInGame(userId, gameId).onFailure { return@run failure(it) }

            // Verify round is in progress (discuss)
            validateRoundInProgress(gameId).onFailure { return@run failure(it) }

            val hand =
                repositoryGame.getPlayerHand(userId, gameId)
                    ?: return@run failure(GameError.EmptyHand)

            success(hand)
        }

    fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ): Either<GameError, Hand> =
        trxManager.run {
            // Validate IDs
            if (userId <= 0) return@run failure(GameError.InvalidUserId)
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Filter out invalid dice indices
            val validLockedDice = lockedDice.filter { it in 0..4 }

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            // Verify user is in the game
            validateUserInGame(userId, gameId).onFailure { return@run failure(it) }

            // Verify round is in progress
            validateRoundInProgress(gameId).onFailure { return@run failure(it) }

            // Verify it's the player's turn
            validatePlayerTurn(userId, gameId).onFailure { return@run failure(it) }

            val currentRollCount = repositoryGame.getRollCount(userId, gameId)
            if (currentRollCount >= 3) {
                return@run failure(GameError.TooManyRolls)
            }

            val currentHand = repositoryGame.getPlayerHand(userId, gameId)?.value?.toMutableList()

            if (currentHand == null) {
                // First roll
                val newHand = Hand(List(5) { createRandomDice() })
                repositoryGame.shuffle(userId, newHand, gameId)
                return@run success(newHand)
            }

            // Validate hand size
            if (currentHand.size != 5) {
                return@run failure(GameError.EmptyHand)
            }

            // Shuffle unlocked dice
            for (diceIdx in currentHand.indices) {
                if (validLockedDice.contains(diceIdx)) continue
                currentHand[diceIdx] = createRandomDice()
            }

            val newHand = Hand(currentHand)
            repositoryGame.shuffle(userId, newHand, gameId)
            success(newHand)
        }

    fun calculatePoints(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Points> =
        trxManager.run {
            // Validate IDs
            if (userId <= 0) return@run failure(GameError.InvalidUserId)
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            // Verify user is in the game
            validateUserInGame(userId, gameId).onFailure { return@run failure(it) }

            // Verify round is in progress
            validateRoundInProgress(gameId).onFailure { return@run failure(it) }

            // Verify it's the player's turn
            validatePlayerTurn(userId, gameId).onFailure { return@run failure(it) }

            val hand =
                repositoryGame.getPlayerHand(userId, gameId)
                    ?: return@run failure(GameError.EmptyHand)

            // Validate hand has dice
            if (hand.value.isEmpty()) {
                return@run failure(GameError.EmptyHand)
            }

            // Validate hand has correct number of dice
            if (hand.value.size != 5) {
                return@run failure(GameError.EmptyHand)
            }

            val score = hand.calculateScore()
            val points = Points(score)

            repositoryGame.calculatePoints(userId, gameId, points)

            success(points)
        }

    fun getRoundWinner(gameId: Int): Either<GameError, RoundWinnerInfo> =
        trxManager.run {
            // Validate game ID
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            // Verify round is in progress
            validateRoundInProgress(gameId).onFailure { return@run failure(it) }

            // Check if all players finished their turns
            val players = repositoryGame.listPlayersInGame(gameId)
            val scoreboard = repositoryGame.getScores(gameId)

            if (scoreboard.pointsQueue.size < players.listPlayersInGame.size) {
                return@run failure(GameError.AllPlayersNotFinished)
            }

            val winner = repositoryGame.getRoundWinner(gameId)
            success(winner)
        }

    fun getGameWinner(gameId: Int): Either<GameError, GameWinnerInfo> =
        trxManager.run {
            // Validate game ID
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            // Get round info
            val roundInfo = repositoryGame.getRoundInfo(gameId)

            // Verify at least one round was played
            if (roundInfo.round.round <= 0) {
                return@run failure(GameError.NoRoundInProgress)
            }

            // Check scoreboard
            val scoreboard = repositoryGame.getScores(gameId)
            if (scoreboard.pointsQueue.isEmpty()) {
                return@run failure(GameError.GameNotFinished)
            }

            // Verify all players finished
            val players = repositoryGame.listPlayersInGame(gameId)
            if (scoreboard.pointsQueue.size < players.listPlayersInGame.size) {
                return@run failure(GameError.AllPlayersNotFinished)
            }

            val winner = repositoryGame.getGameWinner(gameId)
            success(winner)
        }

    fun remainingTime(gameId: Int): Either<GameError, Time> =
        trxManager.run {
            // Validate game ID
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            // Verify round is in progress
            validateRoundInProgress(gameId).onFailure { return@run failure(it) }

            val time = repositoryGame.remainingTime(gameId)
            success(time)
        }

    fun getRoundInfo(gameId: Int): Either<GameError, RoundInfo> =
        trxManager.run {
            // Validate game ID
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            val roundInfo = repositoryGame.getRoundInfo(gameId)
            success(roundInfo)
        }

    fun getScores(gameId: Int): Either<GameError, Scoreboard> =
        trxManager.run {
            // Validate game ID
            if (gameId <= 0) return@run failure(GameError.InvalidGameId)

            // Check if game exists
            val game =
                repositoryGame.findById(gameId)
                    ?: return@run failure(GameError.GameNotFound)

            val scoreboard = repositoryGame.getScores(gameId)
            success(scoreboard)
        }
}
