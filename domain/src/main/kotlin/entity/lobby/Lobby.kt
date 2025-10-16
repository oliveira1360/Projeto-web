package org.example.entity.lobby

import org.example.entity.core.Name
import org.example.entity.player.User

data class Lobby(
    val id: Int,
    val name: Name,
    val hostId: Int,
    val maxPlayers: Int,
    val rounds: Int,
    val currentPlayers: List<User>,
)
