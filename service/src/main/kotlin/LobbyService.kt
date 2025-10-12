package org.example

import jakarta.inject.Named
import org.example.entity.Lobby
import org.example.entity.toName
import java.time.Clock
import java.util.Locale
import java.util.UUID


sealed class LobbyError(val message: String) {
    object LobbyNotFound : LobbyError("Lobby not found")
    object UserNotFound : LobbyError("User not found")
    object InvalidLobbyData : LobbyError("Invalid lobby data")
    object InvalidInviteCode : LobbyError("Invalid invite code")
    object AlreadyInLobby : LobbyError("User already in a lobby")
    object NotInLobby : LobbyError("User not in this lobby")
    object LobbyFull : LobbyError("Lobby is full")
}


typealias LobbyResult = Either<LobbyError, Lobby>

@Named
class LobbyService(
    private val trxManager: TransactionManager,
    private val repositoryLobby: RepositoryLobby,
    private val repositoryUser: RepositoryUser,
    private val config: LobbiesDomainConfig,
    private val clock: Clock
) {

    fun createLobby(hostId: Int, name: String, maxPlayers: Int): LobbyResult {
        if (name.isBlank() || maxPlayers <= 0) {
            return failure(LobbyError.InvalidLobbyData)
        }

        val inviteCode = generateInviteCode()

        return trxManager.run {
            val host = repositoryUser.findById(hostId)
                ?: return@run failure(LobbyError.UserNotFound)

            val lobby = repositoryLobby.createLobby(name.toName(), host.id, maxPlayers, inviteCode)
            success(lobby)
        }
    }

    fun listLobbies(): List<Lobby> =
        trxManager.run {
            repositoryLobby.findAllLobbies()
        }

    fun getLobbyDetails(lobbyId: Int): LobbyResult =
        trxManager.run {
            val lobby = repositoryLobby.findLobbyById(lobbyId)
                ?: return@run failure(LobbyError.LobbyNotFound)
            success(lobby)
        }

    fun joinLobby(userId: Int, inviteCode: String): LobbyResult =
        trxManager.run {
            val user = repositoryUser.findById(userId)
                ?: return@run failure(LobbyError.UserNotFound)

            val lobby = repositoryLobby.findByInviteCode(inviteCode)
                ?: return@run failure(LobbyError.InvalidInviteCode)

            if (repositoryLobby.isUserInLobby(userId, lobby.id)) {
                return@run failure(LobbyError.AlreadyInLobby)
            }

            if (repositoryLobby.countPlayers(lobby.id) >= lobby.maxPlayers) {
                return@run failure(LobbyError.LobbyFull)
            }

            repositoryLobby.addPlayer(lobby.id, user.id)
            val updatedLobby = repositoryLobby.findLobbyById(lobby.id)!!
            success(updatedLobby)
        }

    fun leaveLobby(userId: Int, lobbyId: Int): LobbyResult =
        trxManager.run {
            val lobby = repositoryLobby.findLobbyById(lobbyId)
                ?: return@run failure(LobbyError.LobbyNotFound)

            if (!repositoryLobby.isUserInLobby(userId, lobby.id)) {
                return@run failure(LobbyError.NotInLobby)
            }

            repositoryLobby.removePlayer(lobby.id, userId)

            // Se o host sair, pode fechar o lobby
            if (lobby.hostId == userId) {
                repositoryLobby.closeLobby(lobby.id)
            }

            success(lobby)
        }

    private fun generateInviteCode(): String =
        UUID.randomUUID()
            .toString()
            .substring(0, config.inviteCodeLength.coerceIn(4, 12))
            .uppercase(Locale.getDefault())
}