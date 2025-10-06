package org.example

import jakarta.inject.Named
import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import org.springframework.security.core.token.Token
import org.springframework.security.crypto.password.PasswordEncoder
import java.awt.Image
import java.time.Clock


sealed class UserError {
    data object AlreadyUsedEmailAddress : UserError()

    data object InsecurePassword : UserError()

    data object InvalidCredentials : UserError()
}

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}
@Named
class UserAuthService(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val trxManager: TransactionManager,
    private val clock: Clock,
) {

    fun createUser(name: Name, nickName: Name, email: Email, password: Password, imageUrl: URL?): Either<UserError, User> {
        return trxManager.run {
            val user = repositoryUser.createUser(name,nickName,email, password, imageUrl)
            success(user)
        }

    }

    fun getUserByEmail(email: Email): Either<UserError, User> {
        return trxManager.run {
            val user = repositoryUser.findByEmail(email = email) ?: return@run failure(UserError.InvalidCredentials)
            success(user)
        }
    }
    fun getUserByToken(token: String): User?{
        TODO()

    }

}


