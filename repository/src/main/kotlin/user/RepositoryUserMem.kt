package org.example.user

import org.example.Token
import org.example.TokenValidationInfo
import org.example.entity.Balance
import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import org.example.entity.UserInfo
import org.example.entity.toMoney
import java.time.Instant

class RepositoryUserMem : RepositoryUser {
    private val _users = Companion.users

    val users: List<User> get() = _users

    companion object {
        val users = mutableListOf<User>()
        val tokens = mutableListOf<Token>()
    }

    override fun findByEmail(email: Email): User? = users.find { it.email == email }

    override fun createUser(
        name: Name,
        nickName: Name,
        email: Email,
        password: Password,
        imageUrl: URL?,
    ): User {
        val user =
            User(
                id = users.size + 1,
                name = name,
                nickName = nickName,
                email = email,
                password = password,
                imageUrl = imageUrl,
                balance = Balance(0.toMoney()),
            )
        _users.add(user)
        return user
    }

    override fun findByToken(token: String): User? =
        tokens.find { it.tokenValidationInfo.validationInfo == token }?.let { findById(it.userId) }

    override fun updateUser(
        userId: Int,
        name: Name?,
        nickName: Name?,
        password: Password?,
        imageUrl: URL?,
    ): User =
        users.find { it.id == userId }.let {
            if (it != null) {
                val updatedUser =
                    it.copy(
                        name = name ?: it.name,
                        nickName = nickName ?: it.nickName,
                        password = password ?: it.password,
                        imageUrl = imageUrl ?: it.imageUrl,
                    )
                _users.remove(it)
                _users.add(updatedUser)
                updatedUser
            } else {
                throw IllegalArgumentException("User with id $userId not found")
            }
        }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        tokens.find { it.tokenValidationInfo == tokenValidationInfo }?.let {
            findById(it.userId)?.let { user -> Pair(user, it) }
        }

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        val userTokens = tokens.filter { it.userId == token.userId }
        if (userTokens.size >= maxTokens) {
            val oldestToken = userTokens.minByOrNull { it.createdAt }
            if (oldestToken != null) {
                tokens.remove(oldestToken)
            }
        }
        tokens.add(token)
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        tokens.find { it.tokenValidationInfo == token.tokenValidationInfo }?.let {
            val updatedToken = it.copy(lastUsedAt = now)
            tokens.remove(it)
            tokens.add(updatedToken)
        }
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        tokens
            .removeIf {
                it.tokenValidationInfo == tokenValidationInfo
            }.let { if (it) 1 else 0 }

    override fun userGameInfo(userId: Int): UserInfo {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): User? = users.find { it.id == id }

    override fun findAll(): List<User> = users.toList()

    override fun save(entity: User) {
        val user = findById(entity.id)
        if (user == null) {
            _users.add(entity)
        } else {
            _users[_users.indexOf(user)] = entity
        }
    }

    override fun deleteById(id: Int) {
        RepositoryUserMem.users.removeIf { it.id == id }
    }

    override fun clear() {
        RepositoryUserMem.users.clear()
        tokens.clear()
    }
}
