package org.example.user

import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.URL
import org.example.entity.core.toBalance
import org.example.entity.core.toMoney
import org.example.entity.player.User
import org.example.entity.player.UserInfo
import org.example.token.Token
import org.example.token.TokenValidationInfo
import java.time.Instant

class RepositoryUserMem : RepositoryUser {
    private val _users = Companion.users

    val users: List<User> get() = _users

    companion object {
        val users = mutableListOf<User>()
        val tokens = mutableListOf<Token>()
    }

    override fun findByEmail(email: Email): User? = users.find { it.email == email }

    override fun findByNickName(nickName: Name): User? = users.find { it.nickName == nickName }

    override fun createUser(
        name: Name,
        nickName: Name,
        email: Email,
        passwordHash: String,
        imageUrl: URL?,
    ): User {
        _users.find { it.email == email }?.let {
            throw IllegalArgumentException("User with email $email already exists")
        }
        val user =
            User(
                id = users.size + 1,
                name = name,
                nickName = nickName,
                email = email,
                passwordHash = passwordHash,
                imageUrl = imageUrl,
                balance = Balance(1000.toMoney()),
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
        passwordHash: String?,
        imageUrl: URL?,
    ): User =
        users.find { it.id == userId }.let {
            if (it != null) {
                val updatedUser =
                    it.copy(
                        name = name ?: it.name,
                        nickName = nickName ?: it.nickName,
                        passwordHash = passwordHash ?: it.passwordHash,
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

    override fun findByEmailAndPassword(
        email: Email,
        passwordHash: String,
    ): User? = users.find { it.email == email && it.passwordHash == passwordHash }

    override fun updateBalance(
        userId: Int,
        amount: Int,
    ): Int =
        users.find { it.id == userId }.let { user ->
            if (user != null) {
                val updatedUser = user.copy(balance = (user.balance.money.value + amount).toBalance())
                _users.remove(user)
                _users.add(updatedUser)
                updatedUser.balance.money.value
            } else {
                throw IllegalArgumentException("User with id $userId not found")
            }
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
