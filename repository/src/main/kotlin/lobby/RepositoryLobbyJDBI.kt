package org.example.lobby

import org.example.entity.Balance
import org.example.entity.Lobby
import org.example.entity.Name
import org.example.entity.User
import org.example.entity.toEmail
import org.example.entity.toMoney
import org.example.entity.toName
import org.example.entity.toPassword
import org.example.entity.toUrlOrNull
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class RepositoryLobbyJDBI(
    private val handle: Handle,
) : RepositoryLobby {
    override fun createLobby(
        name: Name,
        hostId: Int,
        maxPlayers: Int,
        rounds: Int,
    ): Lobby {
        val lobbyId =
            handle
                .createUpdate(
                    """
                INSERT INTO lobbies (host_id, name, max_players, rounds)
                VALUES (:host_id, :name, :max_players, :rounds)
                """,
                ).bind("host_id", hostId)
                .bind("name", name.value)
                .bind("max_players", maxPlayers)
                .bind("rounds", rounds)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        // adiciona o host como jogador
        handle
            .createUpdate(
                "INSERT INTO lobby_players (lobby_id, user_id) VALUES (:lobby_id, :user_id)",
            ).bind("lobby_id", lobbyId)
            .bind("user_id", hostId)
            .execute()

        val host =
            handle
                .createQuery("SELECT * FROM users WHERE id = :id")
                .bind("id", hostId)
                .map(UserMapper())
                .one()

        return Lobby(
            id = lobbyId,
            name = name,
            hostId = hostId,
            maxPlayers = maxPlayers,
            rounds = rounds,
            currentPlayers = listOf(host),
        )
    }

    override fun isUserInLobby(
        userId: Int,
        lobbyId: Int,
    ): Boolean {
        val count =
            handle
                .createQuery(
                    """
                SELECT COUNT(*) FROM lobby_players
                WHERE lobby_id = :lobby_id AND user_id = :user_id
                """,
                ).bind("lobby_id", lobbyId)
                .bind("user_id", userId)
                .mapTo(Int::class.java)
                .one()

        return count > 0
    }

    override fun countPlayers(lobbyId: Int): Int =
        handle
            .createQuery("SELECT COUNT(*) FROM lobby_players WHERE lobby_id = :lobby_id")
            .bind("lobby_id", lobbyId)
            .mapTo(Int::class.java)
            .one()

    override fun addPlayer(
        lobbyId: Int,
        userId: Int,
    ) {
        handle
            .createUpdate(
                """
            INSERT INTO lobby_players (lobby_id, user_id)
            VALUES (:lobby_id, :user_id)
            ON CONFLICT DO NOTHING
            """,
            ).bind("lobby_id", lobbyId)
            .bind("user_id", userId)
            .execute()
    }

    override fun removePlayer(
        lobbyId: Int,
        userId: Int,
    ) {
        handle
            .createUpdate(
                "DELETE FROM lobby_players WHERE lobby_id = :lobby_id AND user_id = :user_id",
            ).bind("lobby_id", lobbyId)
            .bind("user_id", userId)
            .execute()
    }

    override fun closeLobby(lobbyId: Int) {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): Lobby? =
        handle
            .createQuery("SELECT * FROM lobbies WHERE id = :id")
            .bind("id", id)
            .map(LobbyMapper(handle))
            .findOne()
            .orElse(null)

    override fun findAll(): List<Lobby> =
        handle
            .createQuery("SELECT * FROM lobbies ORDER BY created_at DESC")
            .map(LobbyMapper(handle))
            .list()

    override fun save(entity: Lobby) {
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
            name = rs.getString("username").toName(),
            nickName = rs.getString("nick_name").toName(),
            email = rs.getString("email").toEmail(),
            imageUrl = rs.getString("avatar_url")?.toUrlOrNull(),
            password = rs.getString("password_hash").toPassword(),
            balance = Balance(rs.getInt("balance").toMoney()),
        )
}

private class LobbyMapper(
    private val handle: Handle,
) : RowMapper<Lobby> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Lobby {
        val lobbyId = rs.getInt("id")
        val hostId = rs.getInt("host_id")

        val players =
            handle
                .createQuery(
                    """
                    SELECT u.* FROM lobby_players lp
                    JOIN users u ON lp.user_id = u.id
                    WHERE lp.lobby_id = :lobby_id
                    """.trimIndent(),
                ).bind("lobby_id", lobbyId)
                .map(UserMapper())
                .list()

        return Lobby(
            id = lobbyId,
            name = rs.getString("name").toName(),
            hostId = hostId,
            maxPlayers = rs.getInt("max_players"),
            rounds = rs.getInt("rounds"),
            currentPlayers = players,
        )
    }
}
