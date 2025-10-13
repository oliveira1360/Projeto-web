package org.example.user

import org.example.Token
import org.example.TokenValidationInfo
import org.example.entity.Balance
import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import org.example.entity.toMoney
import java.time.Instant

class RepositoryUserMem : RepositoryUser {

    private val _users = Companion.users

    val users: List<User> get() = _users
    companion object {
        val users = mutableListOf<User>()
    }

    override fun findByEmail(email: Email): User? {
        TODO("Not yet implemented")
    }

    override fun createUser(
        name: Name,
        nickName: Name,
        email: Email,
        password: Password,
        imageUrl: URL?
    ): User {
        val user = User(
            id = users.size + 1,
            name = name,
            nickName = nickName,
            email = email,
            password = password,
            imageUrl = imageUrl,
            balance = Balance(0.toMoney())
        )
        _users.add(user)
        return user
    }

    override fun findByToken(token: String): User? {
        TODO("Not yet implemented")
    }

    override fun updateUser(
        userId: Int,
        name: Name?,
        nickName: Name?,
        password: Password?,
        imageUrl: URL?
    ): User {
        TODO("Not yet implemented")
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? {
        TODO("Not yet implemented")
    }

    override fun createToken(token: Token, maxTokens: Int) {
        TODO("Not yet implemented")
    }

    override fun updateTokenLastUsed(token: Token, now: Instant) {
        TODO("Not yet implemented")
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): User? {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<User> {
        TODO("Not yet implemented")
    }

    override fun save(entity: User) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Int) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}