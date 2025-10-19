package org.example.game

import org.example.Const
import org.example.entity.PlayerGameInfo
import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Money
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.Points
import org.example.entity.core.URL
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
                jsonb_agg(
                    DISTINCT jsonb_build_object(
                        'id', u.id,
                        'name', u.username,
                        'nickName', u.nick_name,
                        'password_hash', u.password_hash,
                        'email', u.email,
                        'imageUrl', u.avatar_url,
                        'balance', u.balance
                    )
                ) FILTER (WHERE u.id IS NOT NULL) AS players_json,
                COALESCE(
                    (
                        SELECT jsonb_agg(
                            DISTINCT jsonb_build_object(
                                'roundNumber', r2.round_number,
                                'points', (
                                    SELECT jsonb_agg(
                                        jsonb_build_object(
                                            'playerId', t.user_id,
                                            'score', t.score
                                        )
                                    ) 
                                    FROM turn t 
                                    WHERE t.match_id = r2.match_id 
                                    AND t.round_number = r2.round_number
                                )
                            )
                        )
                        FROM rounds r2
                        WHERE r2.match_id = m.id
                    ),
                    '[]'::jsonb
                ) AS rounds_json
            FROM matches m
            LEFT JOIN match_players mp ON mp.match_id = m.id
            LEFT JOIN users u ON u.id = mp.user_id
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
                jsonb_agg(
                    DISTINCT jsonb_build_object(
                        'id', u.id,
                        'name', u.username,
                        'nickName', u.nick_name,
                        'password_hash', u.password_hash,
                        'email', u.email,
                        'imageUrl', u.avatar_url,
                        'balance', u.balance
                    )
                ) FILTER (WHERE u.id IS NOT NULL) AS players_json,
                COALESCE(
                    (
                        SELECT jsonb_agg(
                            DISTINCT jsonb_build_object(
                                'roundNumber', r2.round_number,
                                'points', (
                                    SELECT jsonb_agg(
                                        jsonb_build_object(
                                            'playerId', t.user_id,
                                            'score', t.score
                                        )
                                    ) 
                                    FROM turn t 
                                    WHERE t.match_id = r2.match_id 
                                    AND t.round_number = r2.round_number
                                )
                            )
                        )
                        FROM rounds r2
                        WHERE r2.match_id = m.id
                    ),
                    '[]'::jsonb
                ) AS rounds_json
            FROM matches m
            LEFT JOIN match_players mp ON mp.match_id = m.id
            LEFT JOIN users u ON u.id = mp.user_id
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
    ): Int {
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

        val lobbyPlayers =
            handle
                .createQuery(
                    """
                SELECT user_id 
                FROM lobby_players 
                WHERE lobby_id = :lobbyId
                ORDER BY joined_at
                """,
                ).bind("lobbyId", lobbyId)
                .mapTo(Int::class.java)
                .list()

        // Add all lobby players to the match
        lobbyPlayers.forEachIndexed { index, playerId ->
            handle
                .createUpdate(
                    "INSERT INTO match_players (match_id, user_id, seat_number) VALUES (:matchId, :userId, :seatNumber)",
                ).bind("matchId", matchId)
                .bind("userId", playerId)
                .bind("seatNumber", index + 1)
                .execute()
        }

        val betAmount = Const.MONEY_REMOVE * totalRounds

        handle
            .createUpdate(
                """
            UPDATE users 
            SET balance = balance - :betAmount 
            WHERE id IN (
                SELECT user_id 
                FROM match_players 
                WHERE match_id = :matchId
            )
            """,
            ).bind("betAmount", betAmount)
            .bind("matchId", matchId)
            .execute()

        return matchId
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

    override fun startRound(gameId: Int): Int {
        val roundNumber =
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
                .mapTo(Int::class.java)
                .one()

        handle
            .createUpdate(
                """
            INSERT INTO turn (match_id, round_number, user_id, hand, roll_number, score)
            SELECT :gameId, :roundNumber, user_id, NULL, 0, NULL
            FROM match_players
            WHERE match_id = :gameId
            """,
            ).bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .execute()

        return roundNumber
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
            AND hand IS NOT NULL
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

        val currentRollNumber =
            handle
                .createQuery(
                    """
                SELECT roll_number 
                FROM turn 
                WHERE match_id = :gameId 
                  AND user_id = :userId 
                  AND round_number = :roundNumber
                """,
                ).bind("gameId", gameId)
                .bind("userId", userId)
                .bind("roundNumber", roundNumber)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(0)

        return if (currentRollNumber == 0) {
            handle
                .createQuery(
                    """
                UPDATE turn
                SET 
                    hand = :newHand::diceface[],
                    roll_number = 1
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
        } else {
            handle
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

    override fun hasActiveRound(gameId: Int): Boolean {
        val roundNumber =
            handle
                .createQuery(
                    """
                SELECT MAX(round_number) 
                FROM rounds 
                WHERE match_id = :gameId
                """,
                ).bind("gameId", gameId)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(null)

        return roundNumber != null && roundNumber > 0
    }

    override fun getRoundInfo(gameId: Int): RoundInfo {
        val roundNumber =
            handle
                .createQuery(
                    """
                SELECT round_number 
                FROM rounds 
                WHERE match_id = :gameId 
                ORDER BY round_number DESC 
                LIMIT 1
                """,
                ).bind("gameId", gameId)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(0)

        val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

        return RoundInfo(
            round = Round(roundNumber),
            pointsQueue = pointsQueue,
        )
    }

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
                .map { rs, ctx ->
                    val playerInfo = PlayerGameInfoMapper().map(rs, ctx)
                    val score = rs.getInt("score")
                    Pair(playerInfo, score)
                }.one()

        updateRoundWinner(gameId, roundNumber, winner.first.playerId)

        val hand = winner.first.hand
        val handValue = hand.evaluateHandValue()
        val points = Points(winner.second)

        return RoundWinnerInfo(
            player = winner.first,
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
        // Update rounds table with winner
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

        handle
            .createUpdate(
                """
                UPDATE users 
                SET balance = balance + 2 
                WHERE id = :winnerId
                """,
            ).bind("winnerId", winnerId)
            .execute()

        // Get the winner's score for this round
        val winnerScore =
            handle
                .createQuery(
                    """
                SELECT score 
                FROM turn 
                WHERE match_id = :gameId 
                  AND round_number = :roundNumber 
                  AND user_id = :winnerId
                """,
                ).bind("gameId", gameId)
                .bind("roundNumber", roundNumber)
                .bind("winnerId", winnerId)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(0)

        // Insert or update player_stats for the winner
        handle
            .createUpdate(
                """
                INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
                VALUES (:userId, 0, 0, 0, :points, 0, 0)
                ON CONFLICT (user_id) 
                DO UPDATE SET
                    total_points = player_stats.total_points + :points
                """,
            ).bind("userId", winnerId)
            .bind("points", winnerScore)
            .execute()

        // Update stats for all players in this round (losers)
        handle
            .createUpdate(
                """
                INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
                SELECT 
                    t.user_id,
                    0,
                    0,
                    0,
                    COALESCE(t.score, 0),
                    0,
                    0
                FROM turn t
                WHERE t.match_id = :gameId 
                  AND t.round_number = :roundNumber
                  AND t.user_id != :winnerId
                ON CONFLICT (user_id) 
                DO UPDATE SET
                    total_points = player_stats.total_points + EXCLUDED.total_points
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
        // Update matches table with winner and finish the game
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

        // Get all players in the match and their total scores
        val playersData =
            handle
                .createQuery(
                    """
                SELECT 
                    mp.user_id,
                    COALESCE(SUM(t.score), 0) AS total_score,
                    CASE WHEN mp.user_id = :winnerId THEN 1 ELSE 0 END AS is_winner
                FROM match_players mp
                LEFT JOIN turn t ON t.user_id = mp.user_id AND t.match_id = mp.match_id
                WHERE mp.match_id = :gameId
                GROUP BY mp.user_id
                """,
                ).bind("gameId", gameId)
                .bind("winnerId", winnerId)
                .map { rs, _ ->
                    Triple(
                        rs.getInt("user_id"),
                        rs.getInt("total_score"),
                        rs.getInt("is_winner") == 1,
                    )
                }.list()

        // Update player_stats for all players
        playersData.forEach { (userId, totalScore, isWinner) ->
            if (isWinner) {
                // Winner: increment wins, update streak
                handle
                    .createUpdate(
                        """
                        INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
                        VALUES (:userId, 1, 1, 0, :points, 1, 1)
                        ON CONFLICT (user_id) 
                        DO UPDATE SET
                            total_games = player_stats.total_games + 1,
                            total_wins = player_stats.total_wins + 1,
                            current_streak = player_stats.current_streak + 1,
                            longest_win_streak = GREATEST(player_stats.longest_win_streak, player_stats.current_streak + 1)
                        """,
                    ).bind("userId", userId)
                    .bind("points", totalScore)
                    .execute()
            } else {
                // Loser: increment losses, reset streak
                handle
                    .createUpdate(
                        """
                        INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
                        VALUES (:userId, 1, 0, 1, :points, 0, 0)
                        ON CONFLICT (user_id) 
                        DO UPDATE SET
                            total_games = player_stats.total_games + 1,
                            total_losses = player_stats.total_losses + 1,
                            current_streak = 0
                        """,
                    ).bind("userId", userId)
                    .bind("points", totalScore)
                    .execute()
            }
        }
    }

    override fun getRollCount(
        userId: Int,
        gameId: Int,
    ): Int {
        val roundNumber = getCurrentRoundNumber(userId, gameId) ?: return 0

        return handle
            .createQuery(
                """
                SELECT COALESCE(roll_number, 0) AS roll_count
                FROM turn
                WHERE match_id = :gameId
                  AND user_id = :userId
                  AND round_number = :roundNumber
                """,
            ).bind("gameId", gameId)
            .bind("userId", userId)
            .bind("roundNumber", roundNumber)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)
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
                """,
            ).bind("gameId", gameId)
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
        )

    private fun parsePlayers(json: String): List<User> {
        // JSON format: [{"id": 1, "name": "...", "nickName": "...", "email": "...", "imageUrl": "...", "balance": 1000}, ...]

        if (json == "[]" || json.isBlank()) return emptyList()

        return json
            .removeSurrounding("[", "]")
            .split("},")
            .mapNotNull { playerJson ->
                try {
                    val cleanJson = if (!playerJson.endsWith("}")) "$playerJson}" else playerJson

                    val id = extractJsonValue(cleanJson, "id")?.toIntOrNull() ?: return@mapNotNull null
                    val name = extractJsonValue(cleanJson, "name") ?: return@mapNotNull null
                    val nickName = extractJsonValue(cleanJson, "nickName") ?: return@mapNotNull null
                    val email = extractJsonValue(cleanJson, "email") ?: return@mapNotNull null
                    val imageUrl = extractJsonValue(cleanJson, "imageUrl")
                    val balance = extractJsonValue(cleanJson, "balance")?.toIntOrNull() ?: 0
                    val password = extractJsonValue(cleanJson, "password_hash") ?: ""

                    User(
                        id = id,
                        name = Name(name),
                        nickName = Name(nickName),
                        email = Email(email),
                        imageUrl = imageUrl?.let { URL(it) },
                        password = Password(password),
                        balance = Balance(Money(balance)),
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun parseRounds(json: String): List<RoundInfo> {
        // JSON format: [{"roundNumber": 1, "points": [{"playerId": 1, "score": 28}, ...]}, ...]

        if (json == "[]" || json.isBlank()) return emptyList()

        return json
            .removeSurrounding("[", "]")
            .split("},")
            .mapNotNull { roundJson ->
                try {
                    val cleanJson = if (!roundJson.endsWith("}")) "$roundJson}" else roundJson

                    val roundNumber =
                        extractJsonValue(cleanJson, "roundNumber")?.toIntOrNull()
                            ?: return@mapNotNull null

                    // Extract points array
                    val pointsJson = extractJsonArray(cleanJson, "points") ?: "[]"
                    val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

                    // Parse points for each player
                    if (pointsJson != "[]" && pointsJson != "null") {
                        pointsJson
                            .removeSurrounding("[", "]")
                            .split("},")
                            .forEach { pointJson ->
                                try {
                                    val cleanPointJson = if (!pointJson.endsWith("}")) "$pointJson}" else pointJson
                                    val playerId = extractJsonValue(cleanPointJson, "playerId")?.toIntOrNull()
                                    val score = extractJsonValue(cleanPointJson, "score")?.toIntOrNull()

                                    if (playerId != null && score != null) {
                                        pointsQueue.add(
                                            PointPlayer(
                                                player =
                                                    PlayerGameInfo(
                                                        playerId = playerId,
                                                        rolls = 0.toQuantity(),
                                                        hand = Hand(emptyList()),
                                                        balance = Balance(Money(0)),
                                                    ),
                                                points = Points(score),
                                            ),
                                        )
                                    }
                                } catch (e: Exception) {
                                    // Skip invalid point entries
                                }
                            }
                    }

                    RoundInfo(
                        round = Round(roundNumber),
                        pointsQueue = pointsQueue,
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun extractJsonValue(
        json: String,
        key: String,
    ): String? {
        val pattern = """"$key"\s*:\s*"?([^",}]+)"?""".toRegex()
        return pattern
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.trim()
    }

    private fun extractJsonArray(
        json: String,
        key: String,
    ): String? {
        val pattern = """"$key"\s*:\s*(\[.*?\])""".toRegex()
        return pattern
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.trim()
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
        if (pgArray == null) {
            return Hand(emptyList())
        }
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
