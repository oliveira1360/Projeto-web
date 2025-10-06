package org.example.dto.inputDto

data class CreateUserDTO(
    val name: String,
    val nickName: String,
    val email: String,
    val password: String,
    val imageUrl: String? = null
) {
}

