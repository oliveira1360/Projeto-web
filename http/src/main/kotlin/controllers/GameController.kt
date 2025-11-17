package org.example.controllers

import org.example.Failure
import org.example.GameNotificationService
import org.example.Success
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateGameDTO
import org.example.dto.inputDto.ShuffleDTO
import org.example.game.GameService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

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
    private val gameNotificationService: GameNotificationService,
    private val errorHandler: HandleError,
) {
    @GetMapping("/{gameId}/events")
    fun subscribeToGameEvents(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): SseEmitter {
        val timeout = 86_400_000L // 24 horas
        val emitter = SseEmitter(timeout)
        gameNotificationService.subscribe(user.user.id, gameId, emitter)
        val gameInfo = gameService.getRoundInfo(gameId)
        val firstPlayer =
            when (gameInfo) {
                is Failure -> null
                is Success -> gameInfo.value.roundOrder.firstOrNull()
            }

        try {
            emitter.send(
                SseEmitter
                    .event()
                    .name("connected")
                    .data(
                        mapOf(
                            "message" to "Connected to Game $gameId",
                            "gameId" to gameId,
                            "userId" to user.user.id,
                            "firstPlayer" to firstPlayer,
                        ),
                    ),
            )
        } catch (ex: Exception) {
            emitter.completeWithError(ex)
        }

        return emitter
    }

    /**
     * Creates a new game instance.
     */
    @PostMapping(
        "/create",
        consumes = [ApiMediaTypes.APPLICATION_JSON],
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun createGame(
        user: AuthenticatedUserDto,
        @RequestBody game: CreateGameDTO,
    ): ResponseEntity<*> =
        errorHandler.handleResult("/game", gameService.createGame(user.user.id, game.lobbyId), HttpStatus.CREATED) {
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
            is Failure -> errorHandler.handleGameError(result.value, "/game/$gameId")
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
        errorHandler.handleResult("/game/$gameId/players", gameService.listPlayersInGame(gameId)) {
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
        errorHandler.handleResult("/game/$gameId/round/start", gameService.startRound(gameId), HttpStatus.CREATED) { roundNumber ->
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
        errorHandler.handleResult("/game/$gameId/player/hand", gameService.getPlayerHand(user.user.id, gameId)) {
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
        errorHandler.handleResult("/game/$gameId/player/shuffle", gameService.shuffle(user.user.id, shuffleDTO.lockedDice, gameId)) { it ->
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
        errorHandler.handleResult("/game/$gameId/player/finish", gameService.finishTurn(user.user.id, gameId)) {
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
        errorHandler.handleResult("/game/$gameId/round/winner", gameService.getRoundWinner(gameId)) {
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
    @PatchMapping(
        "/{gameId}/winner",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getGameWinner(
        user: AuthenticatedUserDto,
        @PathVariable gameId: Int,
    ): ResponseEntity<*> =
        errorHandler.handleResult("/game/$gameId/winner", gameService.getGameWinner(gameId)) {
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
        errorHandler.handleResult("/game/$gameId/remaining-time", gameService.remainingTime(gameId)) {
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
        errorHandler.handleResult("/$gameId/round", gameService.getRoundInfo(gameId)) { it ->

            mapOf(
                "round" to it.round,
                "players" to it.pointsQueue.size,
                "order" to
                    it.roundOrder.map { value ->
                        mapOf(
                            "idPlayer" to value,
                        )
                    },
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
        errorHandler.handleResult("/game/$gameId/scores", gameService.getScores(gameId)) { success ->
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
}
