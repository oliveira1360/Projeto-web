package org.example.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Balance
import org.example.entity.core.Money
import org.example.entity.core.Points
import org.example.entity.core.Quantity
import org.example.entity.game.Game
import org.example.entity.game.Round
import org.example.entity.game.RoundInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import org.example.entity.player.PointPlayer
import org.example.entity.player.User
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.sql.Time
import java.util.PriorityQueue

class RepositoryGameJDBI(
    private val handle: Handle,
) : RepositoryGame {
    override fun findById(id: Int): Game? =
        handle
            .createQuery(
                """
            SELECT 
                m.id AS match_id,
                m.pot AS pot,
                json_agg(
                    json_build_object(
                        'id', u.id,
                        'name', u.username,
                        'nickName', u.nick_name,
                        'email', u.email,
                        'imageUrl', u.avatar_url,
                        'balance', u.balance
                    )
                ) AS players_json,
                json_agg(
                    json_build_object(
                        'roundNumber', r.round_number,
                        'points', (
                            SELECT json_agg(
                                json_build_object(
                                    'playerId', h.player_id,
                                    'score', h.score
                                )
                            ) FROM hands h WHERE h.round_id = r.id
                        )
                    )
                ) AS rounds_json
            FROM matches m
            LEFT JOIN match_players mp ON mp.match_id = m.id
            LEFT JOIN users u ON u.id = mp.user_id
            LEFT JOIN rounds r ON r.match_id = m.id
            WHERE m.id = :id
            GROUP BY m.id
            """,
            ).bind("id", id)
            .map(GameMapper())
            .findOne()
            .orElse(null)

    override fun findAll(): List<Game> =
        handle
            .createQuery(
                """
            SELECT 
                m.id AS match_id,
                m.pot AS pot,
                json_agg(
                    DISTINCT jsonb_build_object(
                        'id', u.id,
                        'name', u.username,
                        'nickName', u.nick_name,
                        'email', u.email,
                        'imageUrl', u.avatar_url,
                        'balance', u.balance
                    )
                ) AS players_json,
                json_agg(
                    DISTINCT jsonb_build_object(
                        'roundNumber', r.round_number,
                        'points', (
                            SELECT json_agg(
                                jsonb_build_object(
                                    'playerId', h.player_id,
                                    'score', h.score
                                )
                            ) FROM hands h WHERE h.round_id = r.id
                        )
                    )
                ) AS rounds_json
            FROM matches m
            LEFT JOIN match_players mp ON mp.match_id = m.id
            LEFT JOIN users u ON u.id = mp.user_id
            LEFT JOIN rounds r ON r.match_id = m.id
            GROUP BY m.id
            """,
            ).map(GameMapper())
            .list()

    override fun save(entity: Game) {
        TODO()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM matches WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM matches").execute()
    }

    override fun createGame(
        userId: Int,
        lobbyId: Int,
    ) {
        val totalRounds =
            handle
                .createQuery(
                    "SELECT rounds FROM lobbies WHERE id = :lobbyId",
                ).bind("lobbyId", lobbyId)
                .mapTo(Int::class.java)
                .one()

        val matchId =
            handle
                .createUpdate(
                    "INSERT INTO matches (winner_id, total_rounds, status) VALUES (NULL, :totalRounds, 'ACTIVE')",
                ).bind("totalRounds", totalRounds)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        handle
            .createUpdate(
                "INSERT INTO match_players (match_id, user_id, seat_number) VALUES (:matchId, :userId, 1)",
            ).bind("matchId", matchId)
            .bind("userId", userId)
            .execute()
    }

    override fun closeGame(
        userId: Int,
        gameId: Int,
    ) {
        handle
            .createUpdate(
                "UPDATE matches SET status = 'FINISHED' WHERE id = :gameId",
            ).bind("gameId", gameId)
            .execute()
    }

    override fun listPlayersInGame(gameId: Int): ListPlayersInGame =
        ListPlayersInGame(
            handle
                .createQuery(
                    """
                    SELECT u.id AS player_id,
                           u.username,
                           u.nick_name,
                           u.email,
                           u.avatar_url,
                           u.balance,
                           COALESCE(MAX(dr.roll_number), 0) AS rolls,
                           h.score AS hand_value
                    FROM match_players mp
                    JOIN users u ON mp.user_id = u.id
                    LEFT JOIN rounds r ON r.match_id = mp.match_id
                    LEFT JOIN hands h 
                        ON h.player_id = u.id 
                        AND h.round_id = r.id
                    LEFT JOIN dice_rolls dr 
                        ON dr.player_id = u.id 
                        AND dr.round_id = r.id
                    WHERE mp.match_id = :gameId
                    GROUP BY u.id, h.score
                """,
                ).bind("gameId", gameId)
                .map(PlayerGameInfoMapper())
                .list(),
        )

    override fun startRound(gameId: Int) {
        handle
            .createUpdate(
                """
            INSERT INTO rounds (match_id, round_number)
            VALUES (:gameId, COALESCE(
                (SELECT MAX(round_number) FROM rounds WHERE match_id = :gameId), 0) + 1)
            RETURNING round_number
            """,
            ).bind("gameId", gameId)
            .executeAndReturnGeneratedKeys()
    }

    override fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand? =
        handle
            .createQuery(
                """
            SELECT dice_values, kept_dice FROM dice_rolls
            WHERE player_id = :userId AND round_id = (
                SELECT id FROM rounds WHERE match_id = :gameId ORDER BY round_number DESC LIMIT 1
            )
            """,
            ).bind("userId", userId)
            .bind("gameId", gameId)
            .map(HandMapper())
            .findOne()
            .orElse(null)

    override fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ): Hand =
        handle
            .createUpdate(
                """
        INSERT INTO dice_rolls (round_id, player_id, dice_values, kept_dice)
        VALUES (
            (
                SELECT r.id
                FROM rounds r
                JOIN matches m ON r.match_id = m.id
                WHERE r.match_id = :gameId
                  AND m.status = 'ACTIVE'
                ORDER BY r.round_number DESC
                LIMIT 1
            ),
            :userId,
            ARRAY[1,2,3,4,5], -- TODO
            ARRAY[false,false,false,false,false] -- TODO
        )
        RETURNING dice_values, kept_dice
        """,
            ).bind("userId", userId)
            .bind("gameId", gameId)
            .executeAndReturnGeneratedKeys()
            .map(HandMapper())
            .one()

    override fun calculatePoints(
        userId: Int,
        gameId: Int,
    ): Points? =
        handle
            .createUpdate(
                """
        UPDATE hands
        SET score = 10 -- TODO: calculate actual score
        WHERE player_id = :userId 
          AND round_id = (
              SELECT r.id
              FROM rounds r
              JOIN matches m ON r.match_id = m.id
              WHERE r.match_id = :gameId
                AND m.status = 'ACTIVE'
              ORDER BY r.round_number DESC
              LIMIT 1
          )
        RETURNING score
        """,
            ).bind("userId", userId)
            .bind("gameId", gameId)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .findOne()
            .orElse(null)
            ?.let { Points(it) }

    override fun getRoundWinner(gameId: Int): PlayerGameInfo =
        handle
            .createQuery(
                """
            SELECT player_id, MAX(score) AS score FROM hands
            WHERE round_id = (
                SELECT id FROM rounds WHERE match_id = :gameId ORDER BY round_number DESC LIMIT 1
            )
            GROUP BY player_id
            ORDER BY MAX(score) DESC LIMIT 1
            """,
            ).bind("gameId", gameId)
            .map(PlayerGameInfoMapper())
            .one()

    override fun getGameWinner(gameId: Int): PlayerGameInfo =
        handle
            .createQuery(
                """
            SELECT player_id, SUM(score) AS total_score FROM hands
            WHERE round_id IN (SELECT id FROM rounds WHERE match_id = :gameId)
            GROUP BY player_id
            ORDER BY total_score DESC LIMIT 1
            """,
            ).bind("gameId", gameId)
            .map(PlayerGameInfoMapper())
            .one()

    override fun remainingTime(gameId: Int): Time =
        handle
            .createQuery(
                """
            SELECT EXTRACT(EPOCH FROM (created_at + INTERVAL '30 seconds' - CURRENT_TIMESTAMP)) AS remaining
            FROM matches WHERE id = :gameId
            """,
            ).bind("gameId", gameId)
            .mapTo(Long::class.java)
            .one()
            .let { Time(it) }

    override fun getRoundInfo(gameId: Int): RoundInfo =
        handle
            .createQuery(
                """
            SELECT * FROM rounds WHERE match_id = :gameId ORDER BY round_number DESC LIMIT 1
            """,
            ).bind("gameId", gameId)
            .map(RoundInfoMapper())
            .one()

    override fun getScores(gameId: Int): Scoreboard =
        handle
            .createQuery(
                """
            SELECT player_id, SUM(score) AS total_score FROM hands
            WHERE round_id IN (SELECT id FROM rounds WHERE match_id = :gameId)
            GROUP BY player_id
            """,
            ).bind("gameId", gameId)
            .map(PlayerScoreMapper())
            .list()
            .let { pointPlayers ->
                val queue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })
                queue.addAll(pointPlayers)
                Scoreboard(queue)
            }
}

private class GameMapper : RowMapper<Game> {
    override fun map(
        rs: ResultSet?,
        ctx: StatementContext?,
    ): Game? =
        Game(
            playersGameInfoList = rs?.getString("players_json")?.let { parsePlayers(it) } ?: emptyList(),
            rounds = rs?.getString("rounds_json")?.let { parseRounds(it) } ?: emptyList(),
            pot = Money(rs?.getInt("pot") ?: 0),
        )

    private fun parsePlayers(json: String): List<User> {
        TODO()
    }

    private fun parseRounds(json: String): List<RoundInfo> {
        TODO()
    }
}

private class PlayerGameInfoMapper : RowMapper<PlayerGameInfo> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): PlayerGameInfo {
        val handValue = rs.getInt("hand_value")
        val hand = Hand.entries.find { it.value == handValue } ?: Hand.NO_VALUE

        return PlayerGameInfo(
            playerId = rs.getInt("player_id"),
            rolls = Quantity(rs.getInt("rolls")),
            hands = hand,
            balance = Balance(Money(rs.getDouble("balance").toInt())),
        )
    }
}

private class HandMapper : RowMapper<Hand> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Hand {
        TODO()
        /*
        val diceArray = (rs.getArray("dice_values")?.array as? Array<Int>)?.toList() ?: listOf(0, 0, 0, 0, 0)
        val handRank = evaluateHand(diceArray)
        return handRank
         */
    }

    private fun evaluateHand(dice: List<Int>): Hand {
        val counts =
            dice
                .groupingBy { it }
                .eachCount()
                .values
                .sortedDescending()
        return when {
            counts.contains(5) -> Hand.FIVE_OF_A_KIND
            counts.contains(4) -> Hand.FOUR_OF_A_KIND
            counts.contains(3) && counts.contains(2) -> Hand.FULL_HOUSE
            counts == listOf(1, 1, 1, 1, 1) -> Hand.STRAIGHT
            counts.contains(3) -> Hand.THREE_OF_A_KIND
            counts.count { it == 2 } == 2 -> Hand.TWO_PAIR
            counts.contains(2) -> Hand.ONE_PAIR
            else -> Hand.NO_VALUE
        }
    }
}

private class RoundInfoMapper : RowMapper<RoundInfo> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): RoundInfo {
        val round = Round(rs.getInt("round_number"))
        val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })
        return RoundInfo(round, pointsQueue)
    }
}

private class PlayerScoreMapper : RowMapper<PointPlayer> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): PointPlayer {
        val player =
            PlayerGameInfo(
                playerId = rs.getInt("player_id"),
                rolls = Quantity(3),
                hands = Hand.NO_VALUE,
                balance = Balance(Money(0)),
            )
        val points = Points(rs.getInt("total_score"))
        return PointPlayer(player, points)
    }
}
