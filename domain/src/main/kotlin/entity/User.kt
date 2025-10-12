package org.example.entity


data class User(
    val id: Int,
    val name: Name,
    val nickName: Name,
    val imageUrl: URL?,
    val email: Email,
    val password: Password,
    val balance: Balance,
)
