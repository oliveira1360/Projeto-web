package repository_mock

import org.example.RepositoryLobby
import org.example.entity.Lobby
import org.example.entity.Name

class RepositoryLobbyMock : RepositoryLobby {
    val lobbies = mutableListOf<Lobby>()

    override fun createLobby(
        name: Name,
        hostId: Int,
        maxPlayers: Int,
        inviteCode: String
    ): Lobby {
        val lobby = Lobby(
            id = (lobbies.maxOfOrNull { it.id } ?: 0) + 1,
            name = name,
            hostId = hostId,
            maxPlayers = maxPlayers,
            inviteCode = inviteCode,
            currentPlayers = listOf()
        )
        lobbies.add(lobby)
        return lobby
    }

    override fun findAllLobbies(): List<Lobby> {
        val currLobbies = lobbies.toList()
        return currLobbies
    }

    override fun findLobbyById(id: Int): Lobby? {
        return lobbies.find { it.id == id }
    }

    override fun findByInviteCode(code: String): Lobby? {
        return lobbies.find { it.inviteCode == code }
    }

    override fun isUserInLobby(userId: Int, lobbyId: Int): Boolean {
        val lobby = lobbies.find { it.id == lobbyId}
        return lobby?.currentPlayers?.find { it.id == userId } != null
    }

    override fun countPlayers(lobbyId: Int): Int {
        val lobby = lobbies.find { it.id == lobbyId}
        return lobby?.currentPlayers?.size ?: 0
    }

    override fun addPlayer(lobbyId: Int, userId: Int) {

    }

    override fun removePlayer(lobbyId: Int, userId: Int) {
        lobbies.mapIndexed { idx, lobby ->
            if (lobby.id == lobbyId) {
                val updatedPlayers = lobby.currentPlayers.filter { it.id != userId }
                lobbies[idx] = lobby.copy(currentPlayers = updatedPlayers)
            }
        }
    }

    override fun closeLobby(lobbyId: Int) {
        lobbies.mapIndexed { idx, lobby ->
            if (lobby.id == lobbyId) {
                lobbies[idx] = lobby.copy(currentPlayers = listOf())
            }
        }
    }

    override fun findById(id: Int): Lobby? {
        return lobbies.find { it.id == id }
    }

    override fun findAll(): List<Lobby> {
        return lobbies.toList()
    }

    override fun save(entity: Lobby) {
        val idx = lobbies.indexOfFirst { it.id == entity.id }
        if (idx != -1) {
            lobbies[idx] = entity
        } else {
            lobbies.add(entity)
        }
    }

    override fun deleteById(id: Int) {
        lobbies.removeIf { it.id == id }
    }

    override fun clear() {
        lobbies.clear()
    }

}