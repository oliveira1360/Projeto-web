package org.example.user

import org.example.Repository
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.URL
import org.example.entity.player.User
import org.example.entity.player.UserInfo
import org.example.token.Token
import org.example.token.TokenValidationInfo
import java.time.Instant

interface RepositoryUser : Repository<User> {
    fun findByEmail(email: Email): User?

    fun createUser(
        name: Name,
        nickName: Name,
        email: Email,
        password: Password,
        imageUrl: URL? = null,
    ): User

    fun findByToken(token: String): User?

    fun updateUser(
        userId: Int,
        name: Name?,
        nickName: Name?,
        password: Password?,
        imageUrl: URL?,
    ): User

    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    )

    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int

    fun userGameInfo(userId: Int): UserInfo

    fun findByEmailAndPassword(
        email: Email,
        password: Password,
    ): User?
}
