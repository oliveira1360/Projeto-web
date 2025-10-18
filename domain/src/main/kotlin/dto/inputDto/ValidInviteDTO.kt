package org.example.dto.inputDto

import java.time.Instant

data class ValidInviteDTO(
    val id: Int,
    val token: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val used: Boolean,
)
