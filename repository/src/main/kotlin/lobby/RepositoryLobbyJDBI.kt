package org.example.lobby

import org.example.entity.core.Balance
import org.example.entity.core.Name
import org.example.entity.core.toEmail
import org.example.entity.core.toMoney
import org.example.entity.core.toName
import org.example.entity.core.toPasswordFromHash
import org.example.entity.core.toUrlOrNull
import org.example.entity.lobby.Lobby
import org.example.entity.player.User
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
                INSERT INTO lobbies (host_id, name, max_players, rounds, is_closed)
                VALUES (:host_id, :name, :max_players, :rounds, :is_closed)
                """,
                ).bind("host_id", hostId)
                .bind("name", name.value)
                .bind("max_players", maxPlayers)
                .bind("rounds", rounds)
                .bind("is_closed", false)
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
        handle
            .createUpdate(
                "UPDATE lobbies SET is_closed = TRUE WHERE id = :lobby_id",
            ).bind("lobby_id", lobbyId)
            .execute()
    }

    override fun findLobbiesReadyToStart(
        minPlayers: Int,
        timeoutSeconds: Long,
    ): List<Lobby> {
        val now = java.time.Instant.now()

        val lobbies =
            handle
                .createQuery("SELECT * FROM lobbies ORDER BY created_at DESC")
                .map(LobbyMapper())
                .list()

        if (lobbies.isEmpty()) return emptyList()

        val playersByLobbyId =
            handle
                .createQuery(
                    """
                    SELECT lp.lobby_id, u.*
                    FROM lobby_players lp
                    JOIN users u ON lp.user_id = u.id
                    WHERE lp.lobby_id IN (<ids>)
                    """.trimIndent(),
                ).bindList("ids", lobbies.map { it.id })
                .map { rs, ctx ->
                    rs.getInt("lobby_id") to UserMapper().map(rs, ctx)
                }.groupBy({ it.first }, { it.second })

        return lobbies
            .map { it.copy(currentPlayers = playersByLobbyId[it.id].orEmpty()) }
            .filter { lobby ->
                val currentPlayerCount = lobby.currentPlayers.size
                val timeElapsed =
                    java.time.Duration
                        .between(lobby.createdAt, now)
                        .seconds

                // Lobby está cheio OU tem jogadores mínimos e o timeout passou
                currentPlayerCount >= lobby.maxPlayers ||
                    (currentPlayerCount >= minPlayers && timeElapsed >= timeoutSeconds)
            }
    }

    override fun findById(id: Int): Lobby? {
        val lobby =
            handle
                .createQuery("SELECT * FROM lobbies WHERE id = :id AND is_closed = FALSE")
                .bind("id", id)
                .map(LobbyMapper())
                .findOne()
                .orElse(null) ?: return null

        val players =
            handle
                .createQuery(
                    """
                    SELECT u.*
                    FROM lobby_players lp
                    JOIN users u ON lp.user_id = u.id
                    WHERE lp.lobby_id = :lobby_id
                    """.trimIndent(),
                ).bind("lobby_id", id)
                .map(UserMapper())
                .list()

        return lobby.copy(currentPlayers = players)
    }

    override fun findAll(): List<Lobby> {
        val lobbies =
            handle
                .createQuery("SELECT * FROM lobbies WHERE is_closed = FALSE ORDER BY created_at DESC")
                .map(LobbyMapper())
                .list()

        if (lobbies.isEmpty()) return emptyList()

        val playersByLobbyId =
            handle
                .createQuery(
                    """
                    SELECT lp.lobby_id, u.*
                    FROM lobby_players lp
                    JOIN users u ON lp.user_id = u.id
                    WHERE lp.lobby_id IN (<ids>)
                    """.trimIndent(),
                ).bindList("ids", lobbies.map { it.id })
                .map { rs, ctx ->
                    rs.getInt("lobby_id") to UserMapper().map(rs, ctx)
                }.groupBy({ it.first }, { it.second })

        return lobbies
            .map { it.copy(currentPlayers = playersByLobbyId[it.id].orEmpty()) }
            .filter { it.currentPlayers.size < it.maxPlayers }
    }

    override fun save(entity: Lobby) {
        handle
            .createUpdate(
                """
                UPDATE lobbies
                SET name = :name,
                    host_id = :host_id,
                    max_players = :max_players,
                    rounds = :rounds
                WHERE id = :id
                """,
            ).bind("id", entity.id)
            .bind("name", entity.name.value)
            .bind("host_id", entity.hostId)
            .bind("max_players", entity.maxPlayers)
            .bind("rounds", entity.rounds)
            .execute()

        handle
            .createUpdate("DELETE FROM lobby_players WHERE lobby_id = :lobby_id")
            .bind("lobby_id", entity.id)
            .execute()

        entity.currentPlayers.forEach { player ->
            handle
                .createUpdate(
                    """
                    INSERT INTO lobby_players (lobby_id, user_id)
                    VALUES (:lobby_id, :user_id)
                    """,
                ).bind("lobby_id", entity.id)
                .bind("user_id", player.id)
                .execute()
        }
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM lobby_players WHERE lobby_id = :lobby_id")
            .bind("lobby_id", id)
            .execute()

        handle
            .createUpdate("DELETE FROM lobbies WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM lobby_players").execute()

        handle.createUpdate("DELETE FROM lobbies").execute()
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
            passwordHash = rs.getString("password_hash").toPasswordFromHash(),
            balance = Balance(rs.getInt("balance").toMoney()),
        )
}

private class LobbyMapper(
    private val playersByLobbyId: Map<Int, List<User>> = emptyMap(),
) : RowMapper<Lobby> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Lobby {
        val lobbyId = rs.getInt("id")
        val createdAtTimestamp = rs.getTimestamp("created_at")

        return Lobby(
            id = lobbyId,
            name = rs.getString("name").toName(),
            hostId = rs.getInt("host_id"),
            maxPlayers = rs.getInt("max_players"),
            rounds = rs.getInt("rounds"),
            currentPlayers = playersByLobbyId[lobbyId].orEmpty(),
            createdAt = createdAtTimestamp.toInstant(),
        )
    }
}
