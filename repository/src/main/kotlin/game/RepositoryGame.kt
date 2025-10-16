package org.example.game

import org.example.Repository
import org.example.entity.PlayerGameInfo
import org.example.entity.core.Points
import org.example.entity.game.Game
import org.example.entity.game.RoundInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import java.sql.Time

interface RepositoryGame : Repository<Game> {
    fun createGame(
        userId: Int,
        lobbyId: Int,
    ): Unit

    fun closeGame(
        userId: Int,
        gameId: Int,
    ): Unit

    fun listPlayersInGame(gameId: Int): ListPlayersInGame

    fun startRound(gameId: Int): Unit

    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand?

    fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ): Hand

    fun calculatePoints(
        userId: Int,
        gameId: Int,
    ): Points?

    fun getRoundWinner(gameId: Int): PlayerGameInfo

    fun getGameWinner(gameId: Int): PlayerGameInfo

    fun remainingTime(gameId: Int): Time

    fun getRoundInfo(gameId: Int): RoundInfo

    fun getScores(gameId: Int): Scoreboard
}
