package org.example

import jakarta.inject.Named
import org.example.config.UsersDomainConfig
import org.example.entity.core.Email
import org.example.entity.core.isNameValid
import org.example.entity.core.isUrlValid
import org.example.entity.core.isValidEmail
import org.example.entity.core.toEmail
import org.example.entity.core.toName
import org.example.entity.core.toNameOrNull
import org.example.entity.core.toUrlOrNull
import org.example.entity.player.User
import org.example.entity.player.UserInfo
import org.example.token.Token
import org.example.token.TokenEncoder
import org.example.token.TokenExternalInfo
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64.getUrlDecoder
import java.util.Base64.getUrlEncoder

sealed class UserError {
    data object AlreadyUsedEmailAddress : UserError()

    data object InsecurePassword : UserError()

    data object InvalidCredentials : UserError()
}

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}

typealias ReturnResult = Either<UserError, User>

@Named
class UserAuthService(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val trxManager: TransactionManager,
    private val clock: Clock,
) {
    fun createUser(
        name: String,
        nickName: String,
        email: String,
        passwordHash: String,
        imageUrl: String?,
    ): Either<UserError, User> {
        if (!(
                email.isValidEmail() && name.isNameValid() && nickName.isNameValid() && passwordHash.isNotBlank()
            )
        ) {
            return failure(UserError.InsecurePassword)
        }
        if (imageUrl != null) {
            if (!imageUrl.isUrlValid()) {
                return failure(UserError.InsecurePassword)
            }
        }

        val name = name.toName()
        val nickName = nickName.toName()
        val email = email.toEmail()
        val imageUrl = imageUrl?.toUrlOrNull()

        return trxManager.run {
            val existingUser = repositoryUser.findByEmail(email)?.let { return@run failure(UserError.AlreadyUsedEmailAddress) }

            val existingNickName = repositoryUser.findByNickName(nickName)?.let { return@run failure(UserError.AlreadyUsedEmailAddress) }

            val user = repositoryUser.createUser(name, nickName, email, passwordHash, imageUrl)
            success(user)
        }
    }

    fun getUserByEmail(email: Email): ReturnResult {
        return trxManager.run {
            val user = repositoryUser.findByEmail(email = email) ?: return@run failure(UserError.InvalidCredentials)
            success(user)
        }
    }

    fun getUserByToken(token: String): User? {
        if (!canBeToken(token)) {
            return null
        }
        return trxManager.run {
            val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
            val userAndToken: Pair<User, Token>? = repositoryUser.getTokenByTokenValidationInfo(tokenValidationInfo)
            if (userAndToken != null && isTokenTimeValid(clock, userAndToken.second)) {
                repositoryUser.updateTokenLastUsed(userAndToken.second, clock.instant())
                userAndToken.first
            } else {
                null
            }
        }
    }

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        return trxManager.run {
            repositoryUser.removeTokenByValidationInfo(tokenValidationInfo)
            true
        }
    }

    fun updateUser(
        userID: Int,
        name: String?,
        nickName: String?,
        passwordHash: String?,
        imageUrl: String?,
    ): ReturnResult {
        val name = name.toNameOrNull()
        val nickName = nickName.toNameOrNull()
        val imageUrl = imageUrl.toUrlOrNull()
        return trxManager.run {
            val user = repositoryUser.updateUser(userID, name, nickName, passwordHash, imageUrl)
            success(user)
        }
    }

    fun userStates(userId: Int): ReturnResult {
        return trxManager.run {
            val user = repositoryUser.findById(userId) ?: return@run failure(UserError.InvalidCredentials)
            success(user)
        }
    }

    fun createToken(
        email: String,
        passwordHash: String,
    ): Either<TokenCreationError, TokenExternalInfo> {
        if (!(email.isValidEmail())) {
            return failure(TokenCreationError.UserOrPasswordAreInvalid)
        }

        return trxManager.run {
            val user: User =
                repositoryUser.findByEmailAndPassword(email.toEmail(), passwordHash)
                    ?: return@run failure(TokenCreationError.UserOrPasswordAreInvalid)
            val tokenValue = generateTokenValue()
            val now = clock.instant()
            val newToken =
                Token(
                    tokenEncoder.createValidationInformation(tokenValue),
                    user.id,
                    createdAt = now,
                    lastUsedAt = now,
                )

            repositoryUser.createToken(newToken, config.maxTokensPerUser)
            Either.Right(TokenExternalInfo(tokenValue, getTokenExpiration(newToken)))
        }
    }

    private fun canBeToken(token: String): Boolean =
        try {
            getUrlDecoder().decode(token).size == config.tokenSizeInBytes
        } catch (ex: IllegalArgumentException) {
            false
        }

    private fun isTokenTimeValid(
        clock: Clock,
        token: Token,
    ): Boolean {
        val now = clock.instant()
        return token.createdAt <= now &&
            Duration.between(now, token.createdAt) <= config.tokenTtl &&
            Duration.between(now, token.lastUsedAt) <= config.tokenRollingTtl
    }

    private fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            getUrlEncoder().encodeToString(byteArray)
        }

    private fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt + config.tokenTtl
        val rollingExpiration = token.lastUsedAt + config.tokenRollingTtl
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }

    fun getUserInfo(userId: Int): Either<UserError, UserInfo> {
        return trxManager.run {
            val result = repositoryUser.userGameInfo(userId)

            return@run success(result)
        }
    }
}
