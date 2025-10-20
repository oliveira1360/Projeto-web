@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example.controllers

import org.example.Either
import org.example.Failure
import org.example.LobbyError
import org.example.LobbyNotificationService
import org.example.LobbyService
import org.example.Success
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateLobbyDTO
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/lobbies")
class LobbyController(
    private val lobbyService: LobbyService,
    private val lobbyNotificationService: LobbyNotificationService,
) {
    @GetMapping("/{lobbyId}/events")
    fun subscribeToLobbyEvents(
        user: AuthenticatedUserDto,
        @PathVariable lobbyId: Int,
    ): SseEmitter {
        val emitter = SseEmitter(100_000_000L)

        lobbyNotificationService.subscribe(user.user.id, lobbyId, emitter)

        try {
            emitter.send(
                SseEmitter
                    .event()
                    .name("connected")
                    .data(
                        mapOf(
                            "message" to "Connected to lobby $lobbyId",
                            "lobbyId" to lobbyId,
                            "userId" to user.user.id,
                        ),
                    ),
            )
        } catch (ex: Exception) {
            emitter.completeWithError(ex)
        }

        return emitter
    }

    @PostMapping(
        "/create",
        consumes = [ApiMediaTypes.APPLICATION_JSON],
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun createLobby(
        user: AuthenticatedUserDto,
        @RequestBody body: CreateLobbyDTO,
    ): ResponseEntity<*> =
        handleResult(
            "/lobbies/create",
            lobbyService.createLobby(user.user.id, body.name, body.maxPlayers, body.rounds),
            HttpStatus.CREATED,
        ) {
            mapOf(
                "lobbyId" to it.id,
                "name" to it.name.value,
                "maxPlayers" to it.maxPlayers,
                "currentPlayers" to it.currentPlayers.size,
                "rounds" to it.rounds,
                "_links" to LobbyLinks.createLobby(it.id),
            )
        }

    @GetMapping(
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun listLobbies(): ResponseEntity<*> {
        val result = lobbyService.listLobbies()
        return ResponseEntity.ok(
            mapOf(
                "lobbies" to
                    result.map { lobby ->
                        mapOf(
                            "lobbyId" to lobby.id,
                            "name" to lobby.name.value,
                            "maxPlayers" to lobby.maxPlayers,
                            "currentPlayers" to lobby.currentPlayers.size,
                        )
                    },
                "_links" to LobbyLinks.listLobbies(),
            ),
        )
    }

    @GetMapping(
        "/{lobbyId}",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getLobbyDetails(
        @PathVariable lobbyId: Int,
    ): ResponseEntity<*> =
        handleResult("/lobbies/$lobbyId", lobbyService.getLobbyDetails(lobbyId)) {
            mapOf(
                "lobbyId" to it.id,
                "name" to it.name.value,
                "maxPlayers" to it.maxPlayers,
                "currentPlayers" to it.currentPlayers.size,
                "rounds" to it.rounds,
                "_links" to LobbyLinks.lobbyDetails(lobbyId),
            )
        }

    @PostMapping(
        "/join/{lobbyId}",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun joinLobby(
        user: AuthenticatedUserDto,
        @PathVariable lobbyId: Int,
    ): ResponseEntity<*> =
        handleResult("/lobbies/join/$lobbyId", lobbyService.joinLobby(user.user.id, lobbyId), HttpStatus.ACCEPTED) {
            mapOf(
                "lobbyId" to it.id,
                "message" to "Successfully joined lobby",
                "_links" to LobbyLinks.joinLobby(lobbyId),
            )
        }

    @PostMapping(
        "/leave/{lobbyId}",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun leaveLobby(
        user: AuthenticatedUserDto,
        @PathVariable lobbyId: Int,
    ): ResponseEntity<*> =
        handleResult("/lobbies/leave/$lobbyId", lobbyService.leaveLobby(user.user.id, lobbyId), HttpStatus.ACCEPTED) {
            mapOf(
                "message" to "Successfully left lobby",
                "_links" to LobbyLinks.leaveLobby(),
            )
        }

    private fun handleLobbyError(
        error: LobbyError,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is LobbyError.LobbyNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.LOBBY_NOT_FOUND,
                            title = "Lobby Not Found",
                            status = HttpStatus.NOT_FOUND,
                            detail = "The requested lobby does not exist.",
                            instance = instance,
                        ),
                    )

            is LobbyError.LobbyFull ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.LOBBY_FULL,
                            title = "Lobby Full",
                            status = HttpStatus.CONFLICT,
                            detail = "Cannot join lobby because it has reached maximum capacity.",
                            instance = instance,
                        ),
                    )

            is LobbyError.AlreadyInLobby ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.ALREADY_IN_LOBBY,
                            title = "Already In Lobby",
                            status = HttpStatus.CONFLICT,
                            detail = "Player is already in this lobby.",
                            instance = instance,
                        ),
                    )

            is LobbyError.NotInLobby ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.NOT_IN_LOBBY,
                            title = "Not In Lobby",
                            status = HttpStatus.CONFLICT,
                            detail = "Player is not a member of this lobby.",
                            instance = instance,
                        ),
                    )

            is LobbyError.InvalidLobbyData ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.INVALID_LOBBY_DATA,
                            title = "Invalid Lobby Data",
                            status = HttpStatus.BAD_REQUEST,
                            detail = "The provided lobby data is invalid.",
                            instance = instance,
                        ),
                    )

            is LobbyError.UserNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.USER_NOT_FOUND,
                            title = "User Not Found",
                            status = HttpStatus.NOT_FOUND,
                            detail = "The requested user does not exist.",
                            instance = instance,
                        ),
                    )
        }

    private inline fun <T> handleResult(
        path: String,
        result: Either<LobbyError, T>,
        status: HttpStatus = HttpStatus.OK,
        successBodyBuilder: (T) -> Any,
    ): ResponseEntity<*> =
        when (result) {
            is Failure -> handleLobbyError(result.value, path)
            is Success -> ResponseEntity.status(status).body(successBodyBuilder(result.value))
        }
}
