package org.example.game

import jakarta.inject.Named
import org.example.Either
import org.example.config.GameDomainConfig
import org.example.entity.game.GameWinnerInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame

data class CreatedGame(
    val gameId: Int,
    val status: String = "ACTIVE",
)

@Named
class GameService(
    private val config: GameDomainConfig,
    private val roundService: RoundService,
    private val playerTurnService: PlayerTurnService,
    private val gameEndService: GameEndService,
) {
    fun createGame(
        userId: Int,
        lobbyId: Int,
    ): Either<GameError, CreatedGame> = gameEndService.createGame(userId, lobbyId)

    fun closeGame(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Unit> = gameEndService.closeGame(userId, gameId)

    fun listPlayersInGame(gameId: Int): Either<GameError, ListPlayersInGame> = gameEndService.listPlayersInGame(gameId)

    fun getGameWinner(gameId: Int): Either<GameError, GameWinnerInfo> = gameEndService.getGameWinner(gameId)

    fun getScores(gameId: Int): Either<GameError, Scoreboard> = gameEndService.getScores(gameId)

    // roundService
    fun startRound(gameId: Int) = roundService.startRound(gameId)

    fun getRoundWinner(gameId: Int) = roundService.getRoundWinner(gameId)

    fun getRoundInfo(gameId: Int) = roundService.getRoundInfo(gameId)

    fun remainingTime(gameId: Int) = roundService.remainingTime(gameId)

    // playerTurnService
    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ) = playerTurnService.getPlayerHand(userId, gameId)

    fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ) = playerTurnService.shuffle(userId, lockedDice, gameId)

    fun finishTurn(
        userId: Int,
        gameId: Int,
    ) = playerTurnService.finishTurn(userId, gameId)
}
