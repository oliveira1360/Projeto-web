package org.example

import jakarta.inject.Named
import org.example.entity.PlayerGameInfo
import org.example.entity.core.Points
import org.example.entity.game.RoundInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import java.sql.Time

sealed class GameError {
    data object EmptyHand : GameError()
}

@Named
class GameService(
    private val trxManager: TransactionManager,
) {
    fun createGame(
        userId: Int,
        lobbyId: Int,
    ): Either<GameError, Unit> =
        trxManager.run {
            Success(repositoryGame.createGame(userId, lobbyId))
        }

    fun closeGame(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> =
        trxManager.run {
            Success(repositoryGame.closeGame(userId, gameId))
        }

    fun listPlayersInGame(gameId: Int): Either<GameError, ListPlayersInGame> =
        trxManager.run {
            Success(repositoryGame.listPlayersInGame(gameId))
        }

    fun startRound(gameId: Int): Either<GameError, Unit> =
        trxManager.run {
            Success(repositoryGame.startRound(gameId))
        }

    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Hand> =
        trxManager.run {
            val value = repositoryGame.getPlayerHand(userId, gameId) ?: return@run Failure(GameError.EmptyHand)
            Success(value)
        }

    fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ): Either<GameError, Hand> =
        trxManager.run {
            Success(repositoryGame.shuffle(userId, lockedDice, gameId))
        }

    fun calculatePoints(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Points> =
        trxManager.run {
            val points = repositoryGame.calculatePoints(userId, gameId) ?: return@run Failure(GameError.EmptyHand)
            Success(points)
        }

    fun getRoundWinner(gameId: Int): Either<GameError, PlayerGameInfo> =
        trxManager.run {
            Success(repositoryGame.getRoundWinner(gameId))
        }

    fun getGameWinner(gameId: Int): Either<GameError, PlayerGameInfo> =
        trxManager.run {
            Success(repositoryGame.getGameWinner(gameId))
        }

    fun remainingTime(gameId: Int): Either<GameError, Time> =
        trxManager.run {
            Success(repositoryGame.remainingTime(gameId))
        }

    fun getRoundInfo(gameId: Int): Either<GameError, RoundInfo> =
        trxManager.run {
            Success(repositoryGame.getRoundInfo(gameId))
        }

    fun getScores(gameId: Int): Either<GameError, Scoreboard> =
        trxManager.run {
            Success(repositoryGame.getScores(gameId))
        }
}
