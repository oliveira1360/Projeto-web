package org.example.lobby

import org.example.entity.Lobby
import org.example.entity.Name
import org.jdbi.v3.core.Handle

class RepositoryLobbyJDBI(
    private val handle: Handle,
) : RepositoryLobby {
    override fun createLobby(
        name: Name,
        hostId: Int,
        maxPlayers: Int,
        inviteCode: String,
    ): Lobby {
        TODO("Not yet implemented")
    }

    override fun findAllLobbies(): List<Lobby> {
        TODO("Not yet implemented")
    }

    override fun findLobbyById(id: Int): Lobby? {
        TODO("Not yet implemented")
    }

    override fun findByInviteCode(code: String): Lobby? {
        TODO("Not yet implemented")
    }

    override fun isUserInLobby(
        userId: Int,
        lobbyId: Int,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun countPlayers(lobbyId: Int): Int {
        TODO("Not yet implemented")
    }

    override fun addPlayer(
        lobbyId: Int,
        userId: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun removePlayer(
        lobbyId: Int,
        userId: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun closeLobby(lobbyId: Int) {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): Lobby? {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<Lobby> {
        TODO("Not yet implemented")
    }

    override fun save(entity: Lobby) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Int) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}
