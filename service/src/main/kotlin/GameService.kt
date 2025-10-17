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

    data object UserNotInGame : GameError()

    data object UnauthorizedAction : GameError()
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
            val lobby = repositoryLobby.findById(lobbyId)
                ?: return@run failure(GameError.LobbyNotFound)

            if (!repositoryLobby.isUserInLobby(userId, lobbyId)) {
                return@run failure(GameError.UserNotInGame)
            }

            repositoryGame.createGame(userId, lobbyId)
            success(Unit)
        }

    fun closeGame(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> =
        trxManager.run {
            repositoryGame.closeGame(userId, gameId)
            success(Unit)
        }

    fun listPlayersInGame(gameId: Int): Either<GameError, ListPlayersInGame> =
        trxManager.run {
            val players = repositoryGame.listPlayersInGame(gameId)
            success(players)
        }

    fun startRound(gameId: Int): Either<GameError, Unit> =
        trxManager.run {
            repositoryGame.startRound(gameId)
            success(Unit)
        }

    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Hand> =
        trxManager.run {
            val hand = repositoryGame.getPlayerHand(userId, gameId)
                ?: return@run failure(GameError.EmptyHand)

            success(hand)
        }

    fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ): Either<GameError, Hand> =
        trxManager.run {
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
            repositoryGame.shuffle(userId, newHand, gameId)
            success(newHand)
        }

    fun calculatePoints(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Points> =
        trxManager.run {
            val hand = repositoryGame.getPlayerHand(userId, gameId)
                ?: return@run failure(GameError.EmptyHand)

            val score = hand.calculateScore()
            val points = Points(score)

            repositoryGame.calculatePoints(userId, gameId, points)

            success(points)
        }

    fun getRoundWinner(gameId: Int): Either<GameError, RoundWinnerInfo> =
        trxManager.run {
            val winner = repositoryGame.getRoundWinner(gameId)
            success(winner)
        }

    fun getGameWinner(gameId: Int): Either<GameError, GameWinnerInfo> =
        trxManager.run {
            val winner = repositoryGame.getGameWinner(gameId)
            success(winner)
        }

    fun remainingTime(gameId: Int): Either<GameError, Time> =
        trxManager.run {
            val time = repositoryGame.remainingTime(gameId)
            success(time)
        }

    fun getRoundInfo(gameId: Int): Either<GameError, RoundInfo> =
        trxManager.run {
            val roundInfo = repositoryGame.getRoundInfo(gameId)
            success(roundInfo)
        }

    fun getScores(gameId: Int): Either<GameError, Scoreboard> =
        trxManager.run {
            val scoreboard = repositoryGame.getScores(gameId)
            success(scoreboard)
        }
}