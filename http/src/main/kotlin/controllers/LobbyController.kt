@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example.controllers

import org.example.Either
import org.example.Failure
import org.example.LobbyError
import org.example.LobbyService
import org.example.Success
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateLobbyDTO
import org.example.entity.lobby.Lobby
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/lobbies")
class LobbyController(
    private val lobbyService: LobbyService,
) {
    /*
    curl -X POST "http://localhost:8080/lobbies/create" \
         -H "Content-Type: application/json" \
         -H "Authorization: Bearer <token>" \
         -d '{"name":"Lobby Teste","maxPlayers":4}'
     */
    @PostMapping("/create")
    fun createLobby(
        user: AuthenticatedUserDto,
        @RequestBody body: CreateLobbyDTO,
    ): ResponseEntity<*> {
        val result: Either<LobbyError, Lobby> = lobbyService.createLobby(user.user.id, body.name, body.maxPlayers, body.rounds)

        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
        }
    }

    /*
    curl -X GET "http://localhost:8080/lobbies"
     */
    @GetMapping
    fun listLobbies(): ResponseEntity<*> {
        val result = lobbyService.listLobbies()
        return ResponseEntity.ok(result)
    }

    /*
    curl -X GET "http://localhost:8080/lobbies/{lobbyId}"
     */
    @GetMapping("/{lobbyId}")
    fun getLobbyDetails(
        @PathVariable lobbyId: Int,
    ): ResponseEntity<*> {
        val result: Either<LobbyError, Lobby> = lobbyService.getLobbyDetails(lobbyId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.value)
            is Success -> ResponseEntity.ok(result.value)
        }
    }

    /*
    curl -X POST "http://localhost:8080/lobbies/join/{lobbyId}" \
         -H "Authorization: Bearer <token>"
     */
    @PostMapping("/join/{lobbyId}")
    fun joinLobby(
        user: AuthenticatedUserDto,
        @PathVariable lobbyId: Int,
    ): ResponseEntity<*> {
        val result = lobbyService.joinLobby(user.user.id, lobbyId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.ACCEPTED).body(result.value)
        }
    }

    /*
    curl -X POST "http://localhost:8080/lobbies/leave/{lobbyId}" \
         -H "Authorization: Bearer <token>"
     */
    @PostMapping("/leave/{lobbyId}")
    fun leaveLobby(
        user: AuthenticatedUserDto,
        @PathVariable lobbyId: Int,
    ): ResponseEntity<*> {
        val result = lobbyService.leaveLobby(user.user.id, lobbyId)
        return when (result) {
            is Failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.value)
            is Success -> ResponseEntity.status(HttpStatus.ACCEPTED).body(result.value)
        }
    }
}
