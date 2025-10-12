package org.example.auth

import org.example.UserAuthService
import org.example.dto.inputDto.AuthenticatedUserDto
import org.springframework.stereotype.Component

@Component
class RequestTokenProcessor(
    val usersService: UserAuthService,
) {
    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUserDto? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        val result = usersService.getUserByToken(parts[1])
        return usersService.getUserByToken(parts[1])?.let {
            AuthenticatedUserDto(
                it,
                parts[1],
            )
        }
    }

    companion object {
        const val SCHEME = "bearer"
    }
}