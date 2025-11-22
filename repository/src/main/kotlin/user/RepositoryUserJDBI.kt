package org.example.user

import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.URL
import org.example.entity.core.toBalance
import org.example.entity.core.toEmail
import org.example.entity.core.toName
import org.example.entity.core.toPassword
import org.example.entity.core.toUrlOrNull
import org.example.entity.player.User
import org.example.entity.player.UserInfo
import org.example.token.Token
import org.example.token.TokenValidationInfo
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant

class RepositoryUserJDBI(
    private val handle: Handle,
) : RepositoryUser {
    override fun findByEmail(email: Email): User? =
        handle
            .createQuery("SELECT * FROM users WHERE email = :email")
            .bind("email", email.value)
            .map(UserMapper())
            .findOne()
            .orElse(null)

    override fun findByNickName(nickName: Name): User? =
        handle
            .createQuery("SELECT * FROM users WHERE nick_name = :nickName")
            .bind("nickName", nickName.value)
            .map(UserMapper())
            .findOne()
            .orElse(null)

    override fun createUser(
        name: Name,
        nickName: Name,
        email: Email,
        passwordHash: String,
        imageUrl: URL?,
    ): User {
        val id =
            handle
                .createUpdate(
                    "INSERT INTO users (username, nick_name, email, password_hash, avatar_url) " +
                        "VALUES (:name, :nickName, :email, :password_hash, :imageUrl)",
                ).bind("name", name.value)
                .bind("nickName", nickName.value)
                .bind("email", email.value)
                .bind("password_hash", passwordHash)
                .bind("imageUrl", imageUrl?.value)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        handle
            .createUpdate(
                "INSERT INTO player_stats (user_id) VALUES (:id)",
            ).bind("id", id)
            .execute()

        return User(
            id = id,
            name = name,
            nickName = nickName,
            imageUrl = imageUrl,
            email = email,
            passwordHash = passwordHash,
            balance = 0.toBalance(),
        )
    }

    override fun findByToken(token: String): User? =
        handle
            .createQuery("SELECT * FROM users WHERE token = :token")
            .bind("token", token)
            .map(UserMapper())
            .findOne()
            .orElse(null)

    override fun updateUser(
        userId: Int,
        name: Name?,
        nickName: Name?,
        passwordHash: String?,
        imageUrl: URL?,
    ): User =
        handle
            .createQuery(
                "UPDATE users " +
                    "SET " +
                    "username = COALESCE(:name, username), " +
                    "nick_name = COALESCE(:nickName, nick_name), " +
                    "password_hash = COALESCE(:password, password_hash), " +
                    "avatar_url = COALESCE(:imageUrl, avatar_url) " +
                    "WHERE id = :id " +
                    "RETURNING *",
            ).bind("id", userId)
            .bind("name", name?.value)
            .bind("nickName", nickName?.value)
            .bind("password", passwordHash)
            .bind("imageUrl", imageUrl?.value)
            .map(UserMapper())
            .one()

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        handle
            .createQuery(
                "select * from Users as users " +
                    "inner join Tokens as tokens on users.id = tokens.user_id " +
                    "where tokens.token_validation = :validation_information",
            ).bind("validation_information", tokenValidationInfo.validationInfo)
            .map(UserAndTokenMapper())
            .singleOrNull()

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        // Delete the oldest token when achieved the maximum number of tokens
        val deletions =
            handle
                .createUpdate(
                    """
                    delete from Tokens 
                    where user_id = :user_id 
                        and token_validation in (
                            select token_validation from Tokens where user_id = :user_id 
                                order by last_used_at desc offset :offset
                        )
                    """.trimIndent(),
                ).bind("user_id", token.userId)
                .bind("offset", maxTokens - 1)
                .execute()

        handle
            .createUpdate(
                """
                insert into Tokens(user_id, token_validation, created_at, last_used_at) 
                values (:user_id, :token_validation, :created_at, :last_used_at)
                """.trimIndent(),
            ).bind("user_id", token.userId)
            .bind("token_validation", token.tokenValidationInfo.validationInfo)
            .bind("created_at", token.createdAt.epochSecond)
            .bind("last_used_at", token.lastUsedAt.epochSecond)
            .execute()
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        handle
            .createUpdate(
                """
                update Tokens
                set last_used_at = :last_used_at
                where token_validation = :validation_information
                """.trimIndent(),
            ).bind("last_used_at", now.epochSecond)
            .bind("validation_information", token.tokenValidationInfo.validationInfo)
            .execute()
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        handle
            .createUpdate(
                """
                delete from Tokens
                where token_validation = :validation_information
            """,
            ).bind("validation_information", tokenValidationInfo.validationInfo)
            .execute()

    override fun userGameInfo(userId: Int): UserInfo =
        handle
            .createQuery("select * from player_stats where user_id = :user_id")
            .bind("user_id", userId)
            .map(UserInfoMapper())
            .one()

    override fun findByEmailAndPassword(
        email: Email,
        passwordHash: String,
    ): User? =
        handle
            .createQuery("SELECT * FROM users WHERE email = :email and password_hash = :password_hash")
            .bind("email", email.value)
            .bind("password_hash", passwordHash)
            .map(UserMapper())
            .findOne()
            .orElse(null)

    override fun updateBalance(
        userId: Int,
        amount: Int,
    ): Int =
        handle
            .createQuery(
                "UPDATE users SET balance = balance + :amount WHERE id = :userId RETURNING balance",
            ).bind("amount", amount)
            .bind("userId", userId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(null)

    override fun findById(id: Int): User? =
        handle
            .createQuery("SELECT * FROM users WHERE id = :id")
            .bind("id", id)
            .map(UserMapper())
            .findOne()
            .orElse(null)

    override fun findAll(): List<User> =
        handle
            .createQuery("SELECT * FROM users ORDER BY id")
            .map(UserMapper())
            .list()

    override fun save(entity: User) {
        val exists =
            handle
                .createQuery("SELECT COUNT(*) FROM users WHERE id = :id")
                .bind("id", entity.id)
                .mapTo(Int::class.java)
                .one() > 0

        if (exists) {
            handle
                .createUpdate(
                    """
                    UPDATE users
                    SET username = :name,
                        nick_name = :nickName,
                        email = :email,
                        password_hash = :password,
                        avatar_url = :imageUrl,
                        balance = :balance
                    WHERE id = :id
                    """,
                ).bind("id", entity.id)
                .bind("name", entity.name.value)
                .bind("nickName", entity.nickName.value)
                .bind("email", entity.email.value)
                .bind("password", entity.passwordHash)
                .bind("imageUrl", entity.imageUrl?.value)
                .bind("balance", entity.balance.money.value)
                .execute()
        } else {
            handle
                .createUpdate(
                    """
                    INSERT INTO users (id, username, nick_name, email, password_hash, avatar_url, balance)
                    VALUES (:id, :name, :nickName, :email, :password, :imageUrl, :balance)
                    """,
                ).bind("id", entity.id)
                .bind("name", entity.name.value)
                .bind("nickName", entity.nickName.value)
                .bind("email", entity.email.value)
                .bind("password", entity.passwordHash)
                .bind("imageUrl", entity.imageUrl?.value)
                .bind("balance", entity.balance.money.value)
                .execute()

            handle
                .createUpdate("INSERT INTO player_stats (user_id) VALUES (:id)")
                .bind("id", entity.id)
                .execute()
        }
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM Tokens WHERE user_id = :id")
            .bind("id", id)
            .execute()

        handle
            .createUpdate("DELETE FROM player_stats WHERE user_id = :id")
            .bind("id", id)
            .execute()

        handle
            .createUpdate("DELETE FROM users WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM Tokens").execute()

        handle.createUpdate("DELETE FROM player_stats").execute()

        handle.createUpdate("DELETE FROM lobby_players").execute()

        handle.createUpdate("DELETE FROM match_players").execute()

        handle.createUpdate("DELETE FROM users").execute()
    }

    private data class UserAndTokenModel(
        val id: Int,
        val name: Name,
        val nickName: Name,
        val imageUrl: URL,
        val email: Email,
        val passwordValidation: String,
        val tokenValidation: TokenValidationInfo,
        val balance: Balance,
        val createdAt: Long,
        val lastUsedAt: Long,
    ) {
        val userAndToken: Pair<User, Token>
            get() =
                Pair(
                    User(id, name, nickName, imageUrl, email, passwordValidation, balance),
                    Token(
                        tokenValidation,
                        id,
                        Instant.ofEpochSecond(createdAt),
                        Instant.ofEpochSecond(lastUsedAt),
                    ),
                )
    }
}

private class UserMapper : RowMapper<User> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): User =
        User(
            id = rs.getInt("id"),
            name = rs.getString("username").toName(),
            nickName = rs.getString("nick_name").toName(),
            email = rs.getString("email").toEmail(),
            imageUrl = rs.getString("avatar_url").toUrlOrNull(),
            passwordHash = rs.getString("password_hash"),
            balance = rs.getInt("balance").toBalance(),
        )
}

private class UserInfoMapper : RowMapper<UserInfo> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): UserInfo =
        UserInfo(
            userId = rs.getInt("user_id"),
            totalGamesPlayed = rs.getInt("total_games"),
            totalWins = rs.getInt("total_wins"),
            totalLosses = rs.getInt("total_losses"),
            totalPoints = rs.getInt("total_points"),
            longestStreak = rs.getInt("longest_win_streak"),
            currentStreak = rs.getInt("current_streak"),
        )
}

private class UserAndTokenMapper : RowMapper<Pair<User, Token>> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Pair<User, Token> {
        val user =
            User(
                id = rs.getInt("id"),
                name = rs.getString("username").toName(),
                nickName = rs.getString("nick_name").toName(),
                imageUrl = rs.getString("avatar_url").toUrlOrNull(),
                email = rs.getString("email").toEmail(),
                passwordHash = rs.getString("password_hash"),
                balance = rs.getInt("balance").toBalance(),
            )
        val token =
            Token(
                tokenValidationInfo = TokenValidationInfo(rs.getString("token_validation")),
                userId = rs.getInt("id"),
                createdAt = Instant.ofEpochSecond(rs.getLong("created_at")),
                lastUsedAt = Instant.ofEpochSecond(rs.getLong("last_used_at")),
            )
        return Pair(user, token)
    }
}
