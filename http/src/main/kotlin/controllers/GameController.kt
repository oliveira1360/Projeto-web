package org.example.controllers

import org.example.Failure
import org.example.GameService
import org.example.Success
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateGameDTO
import org.example.dto.inputDto.ShuffleDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/game")
class GameController(
    val gameService: GameService,
) {
    /**
     * Creates a new game instance and registers the requesting player as the host.
     */
    @PostMapping("/create")
    fun createGame(
        user: AuthenticatedUserDto,
        @RequestBody game: CreateGameDTO,
    ): ResponseEntity<*> {
        val result = gameService.createGame(user.user.id, game.lobbyId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Closes an active game, preventing further rounds or actions.
     */
    @GetMapping("/{gameId}/close")
    fun closeGame(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.closeGame(user.user.id, gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Returns a list of all players currently participating in the given game.
     */
    @GetMapping("/{gameId}/players")
    fun listPlayersInGame(
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.listPlayersInGame(gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Starts a new round for the current game.
     */
    @GetMapping("/start/round/{gameId}")
    fun startRound(
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.startRound(gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Returns the current hand of the authenticated player.
     */
    @GetMapping("/{gameId}/player/hand")
    fun getPlayerHand(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.getPlayerHand(user.user.id, gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Allows the player to re-roll (shuffle) their dice once per round.
     * Returns the new dice hand to the player.
     */
    @PostMapping("/shuffle/{gameId}")
    fun shuffle(
        user: AuthenticatedUserDto,
        @RequestBody shuffleDTO: ShuffleDTO,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.shuffle(user.user.id, shuffleDTO.lockedDice, gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Calculates and stores the score for the current player based on their hand.
     * Ends the player’s turn for the round.
     */
    @GetMapping("/{gameId}/player/finish")
    fun calculatePoints(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.calculatePoints(user.user.id, gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Returns the winner of the current round once all players have finished.
     * Also provides their winning combination and points earned.
     */
    @GetMapping("/{gameId}/round/winner")
    fun getRoundWinner(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.getRoundWinner(gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Returns the overall game winner after all rounds have been played.
     * Includes total scores and final ranking of players.
     */
    @GetMapping("/{gameId}/game/winner")
    fun getGameWinner(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.getGameWinner(gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Returns the remaining time (in seconds) before the game auto-starts.
     */
    @GetMapping("/{gameId}/remaining/time")
    fun remainingTime(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.remainingTime(gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Returns the current round’s state and metadata, such as
     * round number, and completed turns.
     */
    @GetMapping("/{gameId}/round")
    fun getRoundInfo(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.getRoundInfo(gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /**
     * Returns the current scoreboard of the game,
     * listing all players and their cumulative points.
     */
    @GetMapping("/{gameId}/scores")
    fun getScores(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> {
        val result = gameService.getScores(gameId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }
}
