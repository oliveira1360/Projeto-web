package org.example

import jakarta.inject.Named
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
    ): Either<GameError, Hand> {
        return trxManager.run {
            val currentHand = repositoryGame.getPlayerHand(userId, gameId)?.value?.toMutableList()

            if (currentHand == null) {
                val newHand = Hand(List(5) { createRandomDice() })
                repositoryGame.shuffle(userId, newHand, gameId)
                return@run success(newHand)
            }

            for (diceIdx in currentHand.indices) {
                if (lockedDice.contains(diceIdx)) continue
                currentHand[diceIdx] = createRandomDice()
            }

            val newHand = Hand(currentHand)
            Success(repositoryGame.shuffle(userId, newHand, gameId))
        }
    }

    fun calculatePoints(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Points> =
        trxManager.run {
            val hand =
                repositoryGame.getPlayerHand(userId, gameId)
                    ?: return@run Failure(GameError.EmptyHand)

            val score = hand.calculateScore()
            val points = Points(score)

            repositoryGame.calculatePoints(userId, gameId, points)

            Success(points)
        }

    fun getRoundWinner(gameId: Int): Either<GameError, RoundWinnerInfo> =
        trxManager.run {
            try {
                Success(repositoryGame.getRoundWinner(gameId))
            } catch (e: IllegalStateException) {
                Failure(GameError.NoRoundInProgress)
            }
        }

    fun getGameWinner(gameId: Int): Either<GameError, GameWinnerInfo> =
        trxManager.run {
            try {
                Success(repositoryGame.getGameWinner(gameId))
            } catch (e: Exception) {
                Failure(GameError.GameNotFinished)
            }
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
