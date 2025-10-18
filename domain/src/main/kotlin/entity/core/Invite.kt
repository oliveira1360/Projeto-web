package org.example.entity.core

import java.time.Instant

data class Invite(
    val id: Int,
    val token: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val used: Boolean,
)
