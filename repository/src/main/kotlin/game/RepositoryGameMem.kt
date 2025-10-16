package org.example.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Points
import org.example.entity.game.Game
import org.example.entity.game.RoundInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import java.sql.Time

class RepositoryGameMem : RepositoryGame {
    override fun createGame(
        userId: Int,
        lobbyId: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun closeGame(
        userId: Int,
        gameId: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun listPlayersInGame(gameId: Int): ListPlayersInGame {
        TODO("Not yet implemented")
    }

    override fun startRound(gameId: Int) {
        TODO("Not yet implemented")
    }

    override fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand {
        TODO("Not yet implemented")
    }

    override fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ): Hand {
        TODO("Not yet implemented")
    }

    override fun calculatePoints(
        userId: Int,
        gameId: Int,
    ): Points {
        TODO("Not yet implemented")
    }

    override fun getRoundWinner(gameId: Int): PlayerGameInfo {
        TODO("Not yet implemented")
    }

    override fun getGameWinner(gameId: Int): PlayerGameInfo {
        TODO("Not yet implemented")
    }

    override fun remainingTime(gameId: Int): Time {
        TODO("Not yet implemented")
    }

    override fun getRoundInfo(gameId: Int): RoundInfo {
        TODO("Not yet implemented")
    }

    override fun getScores(gameId: Int): Scoreboard {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): Game? {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<Game> {
        TODO("Not yet implemented")
    }

    override fun save(entity: Game) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Int) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}
