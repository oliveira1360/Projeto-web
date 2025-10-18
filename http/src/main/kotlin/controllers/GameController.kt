
@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example.controllers

import org.example.Either
import org.example.Failure
import org.example.GameError
import org.example.GameService
import org.example.Success
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateGameDTO
import org.example.dto.inputDto.ShuffleDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * RESTful Game Controller with:
 * - Hypermedia (HATEOAS) links for API discoverability
 * - Problem+JSON error responses (RFC 7807)
 * - Proper HTTP methods and status codes
 * - Content negotiation support
 */
@RestController
@RequestMapping("/game")
class GameController(
    private val gameService: GameService,
) {
    /**
     * Creates a new game instance.
     */
    @PostMapping(
        consumes = [ApiMediaTypes.APPLICATION_JSON],
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun createGame(
        user: AuthenticatedUserDto,
        @RequestBody game: CreateGameDTO,
    ): ResponseEntity<*> =
        handleResult("/game", gameService.createGame(user.user.id, game.lobbyId), HttpStatus.CREATED) {
            val gameId = it.gameId
            mapOf(
                "gameId" to gameId,
                "status" to it.status,
                "_links" to GameLinks.createGame(gameId),
            )
        }

    /**
     * Get game details by ID.
     */
    @GetMapping(
        "/{gameId}",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getGame(
        @PathVariable gameId: Int,
    ): ResponseEntity<*> = TODO()

    /**
     * Closes/deletes a game.
     */
    @PostMapping(
        "/{gameId}",
        produces = [ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun closeGame(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        when (val result = gameService.closeGame(user.user.id, gameId)) {
            is Failure -> handleGameError(result.value, "/game/$gameId")
            is Success -> ResponseEntity.noContent().build<Any>()
        }

    /**
     * List all players in game.
     */
    @GetMapping(
        "/{gameId}/players",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun listPlayersInGame(
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/players", gameService.listPlayersInGame(gameId)) {
            mapOf(
                "players" to it.listPlayersInGame,
                "_links" to GameLinks.listPlayersInGame(gameId),
            )
        }

    /**
     * Start a new round.
     */
    @PostMapping(
        "/{gameId}/round/start",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun startRound(
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/round/start", gameService.startRound(gameId), HttpStatus.CREATED) { roundNumber ->
            mapOf(
                "roundNumber" to roundNumber,
                "message" to "Round $roundNumber started",
                "_links" to GameLinks.startRound(gameId),
            )
        }

    /**
     * Get current player's hand.
     */
    @GetMapping(
        "/{gameId}/player/hand",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getPlayerHand(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/player/hand", gameService.getPlayerHand(user.user.id, gameId)) {
            mapOf(
                "hand" to it.value.map { card -> card.face.name },
                "_links" to GameLinks.playerHand(gameId),
            )
        }

    /**
     * Shuffle/roll dice.
     */
    @PostMapping(
        "/{gameId}/player/shuffle",
        consumes = [ApiMediaTypes.APPLICATION_JSON],
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun shuffle(
        user: AuthenticatedUserDto,
        @RequestBody shuffleDTO: ShuffleDTO,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/player/shuffle", gameService.shuffle(user.user.id, shuffleDTO.lockedDice, gameId)) { it ->
            mapOf(
                "hand" to it.value.map { hand -> hand.face.name },
                "rollNumber" to it.value.size,
                "_links" to GameLinks.shuffle(gameId),
            )
        }

    /**
     * Finish turn and calculate points.
     */
    @PutMapping(
        "/{gameId}/player/finish",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun finishTurn(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/player/finish", gameService.calculatePoints(user.user.id, gameId)) {
            mapOf(
                "points" to it.points,
                "finished" to true,
                "_links" to GameLinks.finishTurn(gameId),
            )
        }

    /**
     * Get round winner.
     */
    @GetMapping(
        "/{gameId}/round/winner",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getRoundWinner(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/round/winner", gameService.getRoundWinner(gameId)) {
            mapOf(
                "winner" to
                    mapOf(
                        "playerId" to it.player.playerId,
                        "points" to it.points.points,
                        "handValue" to it.handValue.name,
                        "roundNumber" to it.roundNumber,
                    ),
                "_links" to GameLinks.getRoundWinner(gameId),
            )
        }

    /**
     * Get game winner.
     */
    @GetMapping(
        "/{gameId}/winner",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getGameWinner(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/winner", gameService.getGameWinner(gameId)) {
            mapOf(
                "winner" to
                    mapOf(
                        "playerId" to it.player.playerId,
                        "totalPoints" to it.totalPoints.points,
                        "roundsWon" to it.roundsWon,
                    ),
                "_links" to GameLinks.getGameWinner(gameId),
            )
        }

    /**
     * Get remaining time.
     */
    @GetMapping(
        "/{gameId}/remaining-time",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun remainingTime(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/remaining-time", gameService.remainingTime(gameId)) {
            mapOf(
                "remainingSeconds" to it.time,
                "_links" to GameLinks.remainingTime(gameId),
            )
        }

    /**
     * Get current round info.
     */
    @GetMapping(
        "/{gameId}/round",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getRoundInfo(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/round", gameService.getRoundInfo(gameId)) {
            mapOf(
                "round" to it.round,
                "players" to it.pointsQueue.size,
                "_links" to GameLinks.getRoundInfo(gameId),
            )
        }

    /**
     * Get scoreboard.
     */
    @GetMapping(
        "/{gameId}/scores",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getScoreboard(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        handleResult("/game/$gameId/scores", gameService.getScores(gameId)) { success ->
            mapOf(
                "players" to
                    success.pointsQueue
                        .sortedByDescending { pointPlayer -> pointPlayer.points.points }
                        .map {
                            mapOf(
                                "playerId" to it.player.playerId,
                                "points" to it.points.points,
                            )
                        },
                "_links" to GameLinks.getScoreboard(gameId),
            )
        }

    /**
     * Handles game errors and converts them to RFC 7807 Problem+JSON responses.
     */
    private fun handleGameError(
        error: GameError,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is GameError.EmptyHand ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.EMPTY_HAND,
                            title = "Empty Hand",
                            status = HttpStatus.BAD_REQUEST,
                            detail = "Player has no hand in the current round. Shuffle first.",
                            instance = instance,
                            additionalProperties = mapOf("requiredAction" to "shuffle"),
                        ),
                    )

            is GameError.NoRoundInProgress ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.NO_ROUND_IN_PROGRESS,
                            title = "No Round In Progress",
                            status = HttpStatus.CONFLICT,
                            detail = "Cannot determine round winner when no round is in progress.",
                            instance = instance,
                        ),
                    )

            is GameError.GameNotFinished ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.GAME_NOT_FINISHED,
                            title = "Game Not Finished",
                            status = HttpStatus.CONFLICT,
                            detail = "Cannot determine game winner until all rounds are completed.",
                            instance = instance,
                            additionalProperties = mapOf("allPlayersFinished" to false),
                        ),
                    )

            is GameError.TooManyRolls ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.TOO_MANY_ROLLS,
                            title = "Too Many Rolls",
                            status = HttpStatus.FORBIDDEN,
                            detail = "Maximum number of rolls (3) exceeded for this round.",
                            instance = instance,
                            additionalProperties = mapOf("maxRolls" to 3, "currentRolls" to 3),
                        ),
                    )

            is GameError.GameNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.GAME_NOT_FOUND,
                            title = "Game Not Found",
                            status = HttpStatus.NOT_FOUND,
                            detail = "The requested game does not exist.",
                            instance = instance,
                        ),
                    )

            else ->
                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.BASE_URI + "/unknown",
                            title = "Unknown Error",
                            status = HttpStatus.INTERNAL_SERVER_ERROR,
                            instance = instance,
                        ),
                    )
        }

    private inline fun <T> handleResult(
        path: String,
        result: Either<GameError, T>,
        status: HttpStatus = HttpStatus.OK,
        successBodyBuilder: (T) -> Any,
    ): ResponseEntity<*> =
        when (result) {
            is Failure -> handleGameError(result.value, path)
            is Success -> ResponseEntity.status(status).body(successBodyBuilder(result.value))
        }
}
