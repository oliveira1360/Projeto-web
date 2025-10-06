package org.example

import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import org.example.entity.toBalance
import org.example.entity.toEmail
import org.example.entity.toName
import org.example.entity.toPassword
import org.example.entity.toUrlOrNull
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet


class RepositoryUserJBDI(
    private val handle: Handle,
): RepositoryUser {
    override fun findByEmail(email: Email): User? =
        handle
            .createQuery("SELECT * FROM users WHERE email = :email")
            .bind("email", email.value)
            .map (UserMapper())
            .findOne()
            .orElse(null)


    override fun createUser(
        name: Name,
        nickName: Name,
        email: Email,
        password: Password,
        imageUrl: URL?,
    ): User {
        val uuid = java.util.UUID.randomUUID()
        val id = handle
            .createUpdate(
                "INSERT INTO users (username, token, nick_name, email, password_hash, avatar_url) " +
                        "VALUES (:name,:token, :nickName, :email, :password, :imageUrl)"
            )
            .bind("name", name.value)
            .bind("token", uuid.toString())
            .bind("nickName", nickName.value)
            .bind("email", email.value)
            .bind("password", password.value)
            .bind("imageUrl", imageUrl?.value)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

        return User(
            id = id,
            uuid = uuid,
            name = name,
            nickName = nickName,
            imageUrl = imageUrl,
            email = email,
            password = password,
            balance = 0.toBalance(),
        )
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

private class UserMapper : RowMapper<User> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): User =
        User(
            id = rs.getInt("id"),
            uuid = rs.getObject("uuid", java.util.UUID::class.java),
            name = rs.getString("name").toName(),
            nickName = rs.getString("nick_name").toName(),
            imageUrl = rs.getString("avatar_url").toUrlOrNull(),
            email = rs.getString("email").toEmail(),
            password = rs.getString("password").toPassword(),
            balance = rs.getInt("balance").toBalance(),
        )
}