package org.example.lobby

import org.example.Repository
import org.example.entity.Lobby
import org.example.entity.Name

interface RepositoryLobby : Repository<Lobby> {
    fun createLobby(
        name: Name,
        hostId: Int,
        maxPlayers: Int,
        inviteCode: String,
    ): Lobby

    fun findAllLobbies(): List<Lobby>

    fun findLobbyById(id: Int): Lobby?

    fun findByInviteCode(code: String): Lobby?

    fun isUserInLobby(
        userId: Int,
        lobbyId: Int,
    ): Boolean

    fun countPlayers(lobbyId: Int): Int

    fun addPlayer(
        lobbyId: Int,
        userId: Int,
    )

    fun removePlayer(
        lobbyId: Int,
        userId: Int,
    )

    fun closeLobby(lobbyId: Int)
}
