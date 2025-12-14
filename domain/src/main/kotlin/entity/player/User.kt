package org.example.entity.player

import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.PasswordHash
import org.example.entity.core.URL

data class User(
    val id: Int,
    val name: Name,
    val nickName: Name,
    val imageUrl: URL?,
    val email: Email,
    val passwordHash: PasswordHash,
    val balance: Balance,
)
