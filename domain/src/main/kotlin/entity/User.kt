package org.example.entity

import java.util.UUID

data class User(
    val id: Int,
    val uuid: UUID,
    val name: Name,
    val nickName: Name,
    val imageUrl: URL?,
    val email: Email,
    val password: Password,
    val balance: Balance,
)
