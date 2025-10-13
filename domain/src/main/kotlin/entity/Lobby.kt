package org.example.entity

data class Lobby(
    val id: Int,
    val name: Name,
    val hostId: Int,
    val maxPlayers: Int,
    val currentPlayers: List<User>,
    val inviteCode: String,
)
