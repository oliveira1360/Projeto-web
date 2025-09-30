package org.example.entity

import java.util.UUID

data class User(
    val uuid: UUID,
    val name: Name,
    val email: Email,
    val password: Password,
)
