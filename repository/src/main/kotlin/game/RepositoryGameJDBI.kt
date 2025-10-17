package org.example.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Balance
import org.example.entity.core.Money
import org.example.entity.core.Points
import org.example.entity.core.toQuantity
import org.example.entity.dice.toDiceFromString
import org.example.entity.game.Game
import org.example.entity.game.GameWinnerInfo
import org.example.entity.game.Round
import org.example.entity.game.RoundInfo
import org.example.entity.game.RoundWinnerInfo
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
                    DISTINCT json_build_object(
                        'id', u.id,
                        'name', u.username,
                        'nickName', u.nick_name,
                        'email', u.email,
                        'imageUrl', u.avatar_url,
                        'balance', u.balance
                    )
                ) AS players_json,
                COALESCE(
                    json_agg(
                        DISTINCT json_build_object(
                            'roundNumber', r.round_number,
                            'points', (
                                SELECT json_agg(
                                    json_build_object(
                                        'playerId', t.user_id,
                                        'score', t.score
                                    )
                                ) 
                                FROM turn t 
                                WHERE t.match_id = r.match_id 
                                AND t.round_number = r.round_number
                            )
                        ) FILTER (WHERE r.round_number IS NOT NULL)
                    ),
                    '[]'::json
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
                COALESCE(
                    json_agg(
                        DISTINCT jsonb_build_object(
                            'roundNumber', r.round_number,
                            'points', (
                                SELECT json_agg(
                                    jsonb_build_object(
                                        'playerId', t.user_id,
                                        'score', t.score
                                    )
                                ) 
                                FROM turn t 
                                WHERE t.match_id = r.match_id 
                                AND t.round_number = r.round_number
                            )
                        ) FILTER (WHERE r.round_number IS NOT NULL)
                    ),
                    '[]'::json
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
                    SELECT u.id AS user_id,
                           u.username,
                           u.nick_name,
                           u.email,
                           u.avatar_url,
                           u.balance,
                           COALESCE(t.roll_number, 0) AS roll_number,
                           t.hand AS hand
                    FROM match_players mp
                    JOIN users u ON mp.user_id = u.id
                    LEFT JOIN LATERAL (
                        SELECT t.roll_number, t.hand
                        FROM turn t
                        WHERE t.match_id = mp.match_id
                          AND t.user_id = u.id
                          AND t.round_number = (
                              SELECT MAX(round_number) 
                              FROM rounds 
                              WHERE match_id = mp.match_id
                          )
                    ) t ON true
                    WHERE mp.match_id = :gameId
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
    ): Hand? {
        val roundNumber = getCurrentRoundNumber(userId, gameId) ?: return null
        return handle
            .createQuery(
                """
            SELECT hand FROM turn
            WHERE user_id = :userId 
            AND match_id = :gameId 
            AND round_number = :roundNumber
            """,
            ).bind("userId", userId)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .map(HandMapper())
            .findOne()
            .orElse(null)
    }

    override fun shuffle(
        userId: Int,
        newHand: Hand,
        gameId: Int,
    ): Hand {
        val roundNumber =
            getCurrentRoundNumber(userId, gameId)
                ?: throw IllegalStateException("No active round found for game $gameId")

        return handle
            .createQuery(
                """
        UPDATE turn
        SET 
            hand = :newHand::diceface[],
            roll_number = roll_number + 1
        WHERE match_id = :gameId
          AND user_id = :userId
          AND round_number = :roundNumber
        RETURNING hand, roll_number
        """,
            ).bind("newHand", newHand.value.map { it.face.name }.toTypedArray())
            .bind("gameId", gameId)
            .bind("userId", userId)
            .bind("roundNumber", roundNumber)
            .map(HandMapper())
            .one()
    }

    override fun calculatePoints(
        userId: Int,
        gameId: Int,
        points: Points,
    ) {
        val roundNumber =
            getCurrentRoundNumber(userId, gameId)
                ?: throw IllegalStateException("No active round found for game $gameId")

        handle
            .createUpdate(
                """
                UPDATE turn
                SET score = :score
                WHERE user_id = :userId 
                  AND match_id = :gameId
                  AND round_number = :roundNumber
                """,
            ).bind("userId", userId)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("score", points.points)
            .execute()
    }

    override fun remainingTime(gameId: Int): Time =
        handle
            .createQuery(
                """
            SELECT EXTRACT(EPOCH FROM (started_at + INTERVAL '30 seconds' - CURRENT_TIMESTAMP)) AS remaining
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

    override fun getRoundWinner(gameId: Int): RoundWinnerInfo {
        val roundNumber =
            getCurrentRoundNumber(0, gameId)
                ?: throw IllegalStateException("No rounds found for game $gameId")

        val winner =
            handle
                .createQuery(
                    """
                SELECT u.id AS user_id, 
                       u.username,
                       u.nick_name,
                       u.email,
                       u.avatar_url,
                       u.balance,
                       t.roll_number,
                       t.hand,
                       t.score
                FROM turn t
                JOIN users u ON t.user_id = u.id
                WHERE t.match_id = :gameId 
                  AND t.round_number = :roundNumber
                  AND t.score IS NOT NULL
                ORDER BY t.score DESC,
                         -- Tiebreaker: sum of dice face weights
                         (SELECT SUM(
                             CASE 
                                 WHEN face = 'ACE' THEN 1
                                 WHEN face = 'KING' THEN 2
                                 WHEN face = 'QUEEN' THEN 3
                                 WHEN face = 'JACK' THEN 4
                                 WHEN face = 'TEN' THEN 5
                                 WHEN face = 'NINE' THEN 6
                             END
                         ) FROM unnest(t.hand) AS face) DESC
                LIMIT 1
                """,
                ).bind("gameId", gameId)
                .bind("roundNumber", roundNumber)
                .map(PlayerGameInfoMapper())
                .one()

        updateRoundWinner(gameId, roundNumber, winner.playerId)

        val hand = winner.hand
        val handValue = hand.evaluateHandValue()
        val points = Points(hand.calculateScore())

        return RoundWinnerInfo(
            player = winner,
            points = points,
            handValue = handValue,
            roundNumber = roundNumber,
        )
    }

    override fun updateRoundWinner(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) {
        handle
            .createUpdate(
                """
                UPDATE rounds
                SET winner_id = :winnerId
                WHERE match_id = :gameId AND round_number = :roundNumber
                """,
            ).bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("winnerId", winnerId)
            .execute()
    }

    override fun getGameWinner(gameId: Int): GameWinnerInfo {
        val winner =
            handle
                .createQuery(
                    """
                WITH player_scores AS (
                    SELECT 
                        u.id AS user_id,
                        u.username,
                        u.nick_name,
                        u.email,
                        u.avatar_url,
                        u.balance,
                        COALESCE(SUM(t.score), 0) AS total_score,
                        COUNT(r.winner_id) FILTER (WHERE r.winner_id = u.id) AS rounds_won
                    FROM match_players mp
                    JOIN users u ON mp.user_id = u.id
                    LEFT JOIN turn t ON t.user_id = u.id AND t.match_id = mp.match_id
                    LEFT JOIN rounds r ON r.match_id = mp.match_id AND r.winner_id = u.id
                    WHERE mp.match_id = :gameId
                    GROUP BY u.id, u.username, u.nick_name, u.email, u.avatar_url, u.balance
                )
                SELECT 
                    user_id,
                    username,
                    nick_name,
                    email,
                    avatar_url,
                    balance,
                    0 AS roll_number,
                    NULL AS hand,
                    total_score,
                    rounds_won
                FROM player_scores
                ORDER BY total_score DESC, rounds_won DESC
                LIMIT 1
                """,
                ).bind("gameId", gameId)
                .map { rs, ctx ->
                    val playerInfo =
                        PlayerGameInfo(
                            playerId = rs.getInt("user_id"),
                            rolls = 0.toQuantity(),
                            hand = Hand(emptyList()),
                            balance = Balance(Money(rs.getInt("balance"))),
                        )
                    Triple(playerInfo, rs.getInt("total_score"), rs.getInt("rounds_won"))
                }.one()

        updateGameWinner(gameId, winner.first.playerId)

        return GameWinnerInfo(
            player = winner.first,
            totalPoints = Points(winner.second),
            roundsWon = winner.third,
        )
    }

    override fun updateGameWinner(
        gameId: Int,
        winnerId: Int,
    ) {
        handle
            .createUpdate(
                """
                UPDATE matches
                SET winner_id = :winnerId,
                    ended_at = CURRENT_TIMESTAMP,
                    status = 'FINISHED'
                WHERE id = :gameId
                """,
            ).bind("gameId", gameId)
            .bind("winnerId", winnerId)
            .execute()
    }

    override fun getScores(gameId: Int): Scoreboard =
        handle
            .createQuery(
                """
            SELECT u.id AS user_id,
                   u.username,
                   u.nick_name,
                   u.email,
                   u.avatar_url,
                   u.balance,
                   0 AS roll_number,
                   NULL AS hand,
                   COALESCE(SUM(t.score), 0) AS score
            FROM match_players mp
            JOIN users u ON mp.user_id = u.id
            LEFT JOIN turn t ON t.user_id = u.id AND t.match_id = mp.match_id
            WHERE mp.match_id = :gameId
            GROUP BY u.id, u.username, u.nick_name, u.email, u.avatar_url, u.balance
            """,
            ).bind("gameId", gameId)
            .map(PlayerScoreMapper())
            .list()
            .let { pointPlayers ->
                val queue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })
                queue.addAll(pointPlayers)
                Scoreboard(queue)
            }

    private fun getCurrentRoundNumber(
        userId: Int,
        gameId: Int,
    ): Int? =
        handle
            .createQuery(
                """
                SELECT MAX(round_number) 
                FROM rounds 
                WHERE match_id = :gameId
                and user_id = :userId
                """,
            ).bind("gameId", gameId)
            .bind("userId", userId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(null)
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
        val playerId = rs.getInt("user_id")
        val roll = rs.getInt("roll_number")
        val balance = Balance(Money(rs.getInt("balance")))
        val pgArray = rs.getArray("hand")
        val diceList =
            if (pgArray != null) {
                val handArray = pgArray.array as Array<*>
                handArray.mapNotNull {
                    try {
                        it?.toString()?.toDiceFromString()
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                emptyList()
            }

        return PlayerGameInfo(
            playerId = playerId,
            rolls = roll.toQuantity(),
            hand = Hand(diceList),
            balance = balance,
        )
    }
}

private class HandMapper : RowMapper<Hand> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Hand {
        val pgArray = rs.getArray("hand")
        val handArray = pgArray.array as Array<*>
        val diceList = handArray.mapNotNull { it?.toString()?.toDiceFromString() }
        return Hand(diceList)
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
    ): PointPlayer =
        PointPlayer(
            player = PlayerGameInfoMapper().map(rs, ctx),
            points = Points(rs.getInt("score")),
        )
}
