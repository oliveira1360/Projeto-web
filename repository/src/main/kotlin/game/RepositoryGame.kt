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
    fun listPlayersInGame(gameId: Int): ListPlayersInGame

    fun getScores(gameId: Int): Scoreboard

    fun remainingTime(gameId: Int): Time

    fun getTotalRoundsOfGame(gameId: Int): Int

    fun getCurrentRoundNumber(gameId: Int): Int?

    fun hasActiveRound(gameId: Int): Boolean

    fun getRoundInfo(gameId: Int): RoundInfo

    fun getRoundOrder(gameId: Int): List<Int>

    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand?

    fun getRollCount(
        userId: Int,
        gameId: Int,
    ): Int

    fun getCurrentPlayerTurn(gameId: Int): Int

    /**
     * Calcula quem seria o vencedor da ronda atual baseado nas pontuações atuais.
     * Não altera o estado da base de dados.
     */
    fun findRoundWinnerCandidate(gameId: Int): RoundWinnerInfo?

    /**
     * Calcula quem seria o vencedor do jogo baseado nas rondas ganhas/pontos.
     * Não altera o estado da base de dados.
     */
    fun findGameWinner(gameId: Int): GameWinnerInfo?

    /**
     * Retorna os dados crus para cálculo de estatísticas finais.
     * @return Lista de Triples: (UserId, TotalScore, IsWinner)
     */
    fun getFinalScoresRaw(
        gameId: Int,
        winnerId: Int,
    ): List<Triple<Int, Int, Boolean>>

    fun getPlayerRoundScore(
        gameId: Int,
        roundNumber: Int,
        userId: Int,
    ): Int

    fun insertMatch(totalRounds: Int): Int

    fun insertMatchPlayer(
        matchId: Int,
        userId: Int,
        seatNumber: Int,
    )

    fun deductBalance(
        matchId: Int,
        amount: Int,
    )

    fun closeGame(gameId: Int)

    fun isGameActive(gameId: Int): Boolean

    fun setGameWinnerAndFinish(
        gameId: Int,
        winnerId: Int,
    )

    fun insertRound(gameId: Int): Int

    fun initTurn(
        gameId: Int,
        roundNumber: Int,
    )

    fun insertRoundOrder(
        gameId: Int,
        roundNumber: Int,
        position: Int,
        userId: Int,
    )

    fun deleteRoundOrder(
        gameId: Int,
        roundNumber: Int,
    )

    fun populateEmptyTurns(
        matchId: Int,
        roundNumber: Int,
        userId: Int,
    )

    fun setRoundWinner(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    )

    fun updateHandAndRoll(
        userId: Int,
        gameId: Int,
        newHand: Hand,
        newRollNumber: Int,
    ): Hand

    fun updateScore(
        userId: Int,
        gameId: Int,
        points: Points,
    )

    fun markTurnAsFinished(
        userId: Int,
        gameId: Int,
    )

    fun rewardPlayer(userId: Int)

    fun updateStatsRoundWinner(
        userId: Int,
        points: Int,
    )

    fun updateStatsRoundLosers(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    )

    fun setRoundOrder(
        gameId: Int,
        roundNumber: Int,
        playerOrder: List<Int>,
    )

    fun updateStatsGameWinner(
        userId: Int,
        points: Int,
    )

    fun updateStatsGameLoser(
        userId: Int,
        points: Int,
    )

    fun removePlayerFromGame(
        gameId: Int,
        userId: Int,
    )
}
