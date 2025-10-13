package org.example.user

import org.example.Repository
import org.example.Token
import org.example.TokenValidationInfo
import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import java.time.Instant

interface RepositoryUser : Repository<User> {
    fun findByEmail(email: Email): User?
    fun createUser(name: Name, nickName: Name, email: Email, password: Password, imageUrl: URL? = null): User
    fun findByToken(token: String): User?
    fun updateUser(userId: Int, name: Name?, nickName: Name?, password: Password?, imageUrl: URL?): User
    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?
    fun createToken(token: Token, maxTokens: Int)
    fun updateTokenLastUsed(token: Token, now: Instant )
    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int

}