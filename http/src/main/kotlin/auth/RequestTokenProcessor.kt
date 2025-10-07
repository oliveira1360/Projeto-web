package org.example.auth

import org.example.Either
import org.example.UserAuthService
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.entity.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
        return when(result){
            is Either.Left<*> -> null
            is Either.Right<*> -> {
                val user = result.value as User
                AuthenticatedUserDto(user.id, user.uuid.toString(), user.name.value,
                    user.nickName.value, user.imageUrl?.value,
                    user.email.value, user.password.value, user.balance.money.value)
            }
        }
    }

    companion object {
        const val SCHEME = "bearer"
    }
}