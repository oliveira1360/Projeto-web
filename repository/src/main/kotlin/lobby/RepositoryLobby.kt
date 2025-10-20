package org.example.lobby

import org.example.Repository
import org.example.entity.core.Name
import org.example.entity.lobby.Lobby

interface RepositoryLobby : Repository<Lobby> {
    fun createLobby(
        name: Name,
        hostId: Int,
        maxPlayers: Int,
        rounds: Int,
    ): Lobby

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

    fun findLobbiesReadyToStart(minPlayers: Int, timeoutSeconds: Long): List<Lobby>
}
