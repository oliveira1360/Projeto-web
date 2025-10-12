package org.example.dto.inputDto

data class UpdateUserDTO(
    val name: String? = null,
    val nickName: String? = null,
    val password: String? = null,
    val imageUrl: String? = null,
) {
}