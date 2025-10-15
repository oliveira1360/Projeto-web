package org.example.dto.inputDto

data class CreateLobbyDTO(
    val name: String,
    val maxPlayers: Int,
    val rounds: Int,
)
