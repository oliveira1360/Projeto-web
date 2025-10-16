package org.example.lobby

import org.example.entity.core.Name
import org.example.entity.lobby.Lobby
import org.example.user.RepositoryUserMem

class RepositoryLobbyMem : RepositoryLobby {
    companion object {
        val userRepo = RepositoryUserMem()
        val lobbies = mutableListOf<Lobby>()
    }

    private val compLobbyList = Companion.lobbies

    override fun createLobby(
        name: Name,
        hostId: Int,
        maxPlayers: Int,
        rounds: Int,
    ): Lobby {
        val lobby =
            Lobby(
                id = 1,
                name = name,
                hostId = hostId,
                maxPlayers = maxPlayers,
                rounds = rounds,
                currentPlayers = listOf(userRepo.findById(hostId) ?: throw IllegalArgumentException("Invalid hostId: $hostId")),
            )
        lobbies.add(lobby)
        return lobby
    }

    override fun isUserInLobby(
        userId: Int,
        lobbyId: Int,
    ): Boolean {
        val lobby = findById(lobbyId) ?: return false
        return lobby.currentPlayers.any { it.id == userId }
    }

    override fun countPlayers(lobbyId: Int): Int {
        val lobby = findById(lobbyId) ?: return 0
        return lobby.currentPlayers.size
    }

    override fun addPlayer(
        lobbyId: Int,
        userId: Int,
    ) {
        val lobby = findById(lobbyId) ?: throw IllegalArgumentException("Lobby not found: $lobbyId")
        val user = userRepo.findById(userId) ?: throw IllegalArgumentException("User not found: $userId")
        if (lobby.currentPlayers.size >= lobby.maxPlayers) {
            throw IllegalStateException("Lobby is full: $lobbyId")
        }
        if (isUserInLobby(userId, lobbyId)) {
            throw IllegalStateException("User already in lobby: $userId")
        }
        lobbies[lobbies.indexOf(lobby)] =
            lobby.copy(
                currentPlayers = lobby.currentPlayers + user,
            )
    }

    override fun removePlayer(
        lobbyId: Int,
        userId: Int,
    ) {
        val lobby = findById(lobbyId) ?: throw IllegalArgumentException("Lobby not found: $lobbyId")
        if (!isUserInLobby(userId, lobbyId)) {
            throw IllegalStateException("User not in lobby: $userId")
        }
        lobbies[lobbies.indexOf(lobby)] =
            lobby.copy(
                currentPlayers = lobby.currentPlayers.filter { it.id != userId },
            )
    }

    override fun closeLobby(lobbyId: Int) {
      /*
      val lobby = findById(lobbyId) ?: throw IllegalArgumentException("Lobby not found: $lobbyId")
        lobbies[lobbies.indexOf(lobby)] =
            lobby.copy(
                isClosed = true,
            )
       */
    }

    override fun findById(id: Int): Lobby? = lobbies.find { it.id == id }

    override fun findAll(): List<Lobby> = lobbies.toList()

    override fun save(entity: Lobby) {
        val lobby = findById(entity.id)
        if (lobby == null) {
            lobbies.add(entity)
        } else {
            lobbies[lobbies.indexOf(lobby)] = entity
        }
    }

    override fun deleteById(id: Int) {
        val lobby = findById(id) ?: throw IllegalArgumentException("Lobby not found: $id")
        lobbies.remove(lobby)
    }

    override fun clear() {
        lobbies.clear()
    }
}
