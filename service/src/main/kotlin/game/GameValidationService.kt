package org.example.game

import jakarta.inject.Named
import org.example.Either
import org.example.Transaction
import org.example.andThen
import org.example.entity.game.RoundInfo
import org.example.failure
import org.example.success

@Named
class GameValidationService {
    /**
     * Validates that the user is a player in the specified game.
     */
    fun Transaction.validateUserInGame(
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
    fun Transaction.validateRoundInProgress(gameId: Int): Either<GameError, RoundInfo> {
        val roundInfo = repositoryGame.getRoundInfo(gameId)

        return if (roundInfo.round.round <= 0) {
            failure(GameError.RoundNotStarted)
        } else {
            success(roundInfo)
        }
    }

    /**
     * Validates that it's the player's turn
     */
    fun Transaction.validatePlayerTurn(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> {
        // TODO
        return success(Unit)
    }

    /**
     * Validates game ID
     */
    fun validateGameId(gameId: Int): Either<GameError, Unit> = if (gameId <= 0) failure(GameError.InvalidGameId) else success(Unit)

    /**
     * Validates user ID
     */
    fun validateUserId(userId: Int): Either<GameError, Unit> = if (userId <= 0) failure(GameError.InvalidUserId) else success(Unit)

    /**
     * Checks if game exists
     */
    fun Transaction.checkGameExists(gameId: Int): Either<GameError, Unit> =
        if (repositoryGame.findById(gameId) != null) {
            success(Unit)
        } else {
            failure(GameError.GameNotFound)
        }

    fun Transaction.validateBasicGameAccess(gameId: Int): Either<GameError, Unit> =
        validateGameId(gameId)
            .andThen { checkGameExists(gameId) }

    fun Transaction.validatePlayerGameAccess(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> =
        validateUserId(userId)
            .andThen { validateGameId(gameId) }
            .andThen { checkGameExists(gameId) }
            .andThen { validateUserInGame(userId, gameId) }
}
