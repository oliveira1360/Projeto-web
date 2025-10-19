package org.example

import jakarta.inject.Named
import org.example.config.LobbiesDomainConfig
import org.example.entity.core.toName
import org.example.entity.lobby.Lobby
import java.util.UUID

sealed class LobbyError(
    val message: String,
) {
    object LobbyNotFound : LobbyError("Lobby not found")

    object UserNotFound : LobbyError("User not found")

    object InvalidLobbyData : LobbyError("Invalid lobby data")

    object AlreadyInLobby : LobbyError("User already in a lobby")

    object NotInLobby : LobbyError("User not in this lobby")

    object LobbyFull : LobbyError("Lobby is full")
}

typealias LobbyResult = Either<LobbyError, Lobby>

@Named
class LobbyService(
    private val trxManager: TransactionManager,
    private val config: LobbiesDomainConfig,
) {
    fun createLobby(
        hostId: Int,
        name: String,
        maxPlayers: Int,
        rounds: Int,
    ): LobbyResult {
        if (name.isBlank() || maxPlayers <= 0) {
            return failure(LobbyError.InvalidLobbyData)
        }
        return trxManager.run {
            val lobby = repositoryLobby.createLobby(name.toName(), hostId, maxPlayers, rounds)
            success(lobby)
        }
    }

    fun listLobbies(): List<Lobby> =
        trxManager.run {
            repositoryLobby.findAll()
        }

    fun getLobbyDetails(lobbyId: Int): LobbyResult =
        trxManager.run {
            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)
            success(lobby)
        }

    fun joinLobby(
        userId: Int,
        lobbyId: Int,
    ): LobbyResult =
        trxManager.run {
            val user =
                repositoryUser.findById(userId)
                    ?: return@run failure(LobbyError.UserNotFound)

            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)

            if (repositoryLobby.isUserInLobby(userId, lobby.id)) {
                return@run failure(LobbyError.AlreadyInLobby)
            }

            if (repositoryLobby.countPlayers(lobby.id) >= lobby.maxPlayers) {
                return@run failure(LobbyError.LobbyFull)
            }

            repositoryLobby.addPlayer(lobby.id, user.id)
            val updatedLobby = repositoryLobby.findById(lobby.id)!!
            success(updatedLobby)
        }

    fun leaveLobby(
        userId: Int,
        lobbyId: Int,
    ): LobbyResult =
        trxManager.run {
            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)

            if (!repositoryLobby.isUserInLobby(userId, lobby.id)) {
                return@run failure(LobbyError.NotInLobby)
            }

            // Captura o estado do lobby antes de remover
            val isHost = lobby.hostId == userId
            val updatedLobby = lobby.copy(
                currentPlayers = lobby.currentPlayers.filter { it.id != userId }
            )

            repositoryLobby.removePlayer(lobby.id, userId)

            // Se o host sair, fecha o lobby
            if (isHost) {
                repositoryLobby.closeLobby(lobby.id)
            }

            success(updatedLobby)
        }

    private fun generateInviteCode(): String =
        UUID
            .randomUUID()
            .toString()
            .substring(0, 4)
}