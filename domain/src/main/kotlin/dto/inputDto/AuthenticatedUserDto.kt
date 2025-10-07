package org.example.dto.inputDto

import org.example.entity.Balance
import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import java.util.UUID

class AuthenticatedUserDto(
    val id: Int,
    val uuid: String,
    val name: String,
    val nickName: String,
    val imageUrl: String?,
    val email: String,
    val password: String,
    val balance: Int,
) {
}