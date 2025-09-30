package org.example.entity

import java.util.UUID

data class Player(
    val uuid: UUID,
    val name: Name,
    val balance: Balance,
)
