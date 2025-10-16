package org.example.dto.inputDto

import org.example.entity.player.User

class AuthenticatedUserDto(
    val user: User,
    val token: String,
)
