package org.example.game

import org.example.Repository
import org.example.entity.core.Points
import org.example.entity.game.Game
import org.example.entity.game.GameWinnerInfo
import org.example.entity.game.RoundInfo
import org.example.entity.game.RoundWinnerInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import java.sql.Time

interface RepositoryGame : Repository<Game> {
    fun createGame(
        userId: Int,
        lobbyId: Int,
    ): Int

    fun closeGame(gameId: Int): Unit

    fun listPlayersInGame(gameId: Int): ListPlayersInGame

    fun startRound(gameId: Int): Int

    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand?

    fun shuffle(
        userId: Int,
        newHand: Hand,
        gameId: Int,
    ): Hand

    fun updateScore(
        userId: Int,
        gameId: Int,
        points: Points,
    ): Unit

    fun getRoundWinner(gameId: Int): RoundWinnerInfo

    fun updateRoundWinner(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ): Unit

    fun getGameWinner(gameId: Int): GameWinnerInfo

    fun updateGameWinner(
        gameId: Int,
        winnerId: Int,
    ): Unit

    fun getRollCount(
        userId: Int,
        gameId: Int,
    ): Int

    fun remainingTime(gameId: Int): Time

    fun hasActiveRound(gameId: Int): Boolean

    fun getRoundInfo(gameId: Int): RoundInfo

    fun getRoundOrder(gameId: Int): List<Int>

    fun setRoundOrder(
        gameId: Int,
        roundNumber: Int,
        playerOrder: List<Int>,
    ): Unit

    fun getScores(gameId: Int): Scoreboard

    fun getCurrentRoundNumber(gameId: Int): Int?

    fun getTotalRoundsOfGame(gameId: Int): Int

    fun populateEmptyTurns(
        matchId: Int,
        roundNumber: Int,
    )

    fun getCurrentPlayerTurn(gameId: Int): Int
}
