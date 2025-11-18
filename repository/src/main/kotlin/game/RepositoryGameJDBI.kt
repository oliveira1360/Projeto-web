package org.example.game

import org.example.Const
import org.example.entity.PlayerGameInfo
import org.example.entity.core.Balance
import org.example.entity.core.Money
import org.example.entity.core.Name
import org.example.entity.core.Points
import org.example.entity.core.toQuantity
import org.example.entity.game.Game
import org.example.entity.game.GameWinnerInfo
import org.example.entity.game.Round
import org.example.entity.game.RoundInfo
import org.example.entity.game.RoundWinnerInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import org.example.entity.player.PointPlayer
import org.example.game.mappers.GameMapper
import org.example.game.mappers.HandMapper
import org.example.game.mappers.PlayerGameInfoMapper
import org.example.game.mappers.PlayerScoreMapper
import org.jdbi.v3.core.Handle
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

    override fun closeGame(gameId: Int) {
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
        ORDER BY seat_number
        """,
            ).bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .execute()

        // Create round order based on seat number
        val playerOrder =
            handle
                .createQuery(
                    """
            SELECT user_id 
            FROM match_players
            WHERE match_id = :gameId
            ORDER BY seat_number
            """,
                ).bind("gameId", gameId)
                .mapTo(Int::class.java)
                .list()

        playerOrder.forEachIndexed { index, userId ->
            handle
                .createUpdate(
                    """
            INSERT INTO round_order (match_id, round_number, order_position, user_id)
            VALUES (:gameId, :roundNumber, :position, :userId)
            """,
                ).bind("gameId", gameId)
                .bind("roundNumber", roundNumber)
                .bind("position", index + 1)
                .bind("userId", userId)
                .execute()
        }

        return roundNumber
    }

    override fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand? {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return null
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
            getCurrentRoundNumber(gameId)
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

        val newRollNumber = if (currentRollNumber == 0) 1 else currentRollNumber + 1

        println("shuffle(): gameId=$gameId, userId=$userId, round=$roundNumber")

        return handle
            .createQuery(
                """
        UPDATE turn
        SET 
            hand = :newHand::diceface[],
            roll_number = :newRollNumber
        WHERE match_id = :gameId
          AND user_id = :userId
          AND round_number = :roundNumber
        RETURNING hand, roll_number
        """,
            ).bind("newHand", newHand.value.map { it.face.name }.toTypedArray())
            .bind("newRollNumber", newRollNumber)
            .bind("gameId", gameId)
            .bind("userId", userId)
            .bind("roundNumber", roundNumber)
            .map(HandMapper())
            .one()
    }

    override fun updateScore(
        userId: Int,
        gameId: Int,
        points: Points,
    ) {
        val roundNumber =
            getCurrentRoundNumber(gameId)
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

        val roundOrder = getRoundOrder(gameId)

        val turn = getCurrentPlayerTurn(gameId)

        return RoundInfo(
            round = Round(roundNumber),
            pointsQueue = pointsQueue,
            roundOrder = roundOrder,
            turn = turn,
        )
    }

    override fun getRoundOrder(gameId: Int): List<Int> {
        val roundNumber =
            getCurrentRoundNumber(gameId)
                ?: return emptyList()

        return handle
            .createQuery(
                """
        SELECT user_id
        FROM round_order
        WHERE match_id = :gameId AND round_number = :roundNumber
        ORDER BY order_position
        """,
            ).bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .mapTo(Int::class.java)
            .list()
    }

    override fun setRoundOrder(
        gameId: Int,
        roundNumber: Int,
        playerOrder: List<Int>,
    ) {
        // Delete existing order for this round
        handle
            .createUpdate(
                """
        DELETE FROM round_order
        WHERE match_id = :gameId AND round_number = :roundNumber
        """,
            ).bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .execute()

        playerOrder.forEachIndexed { index, userId ->
            handle
                .createUpdate(
                    """
            INSERT INTO round_order (match_id, round_number, order_position, user_id)
            VALUES (:gameId, :roundNumber, :position, :userId)
            """,
                ).bind("gameId", gameId)
                .bind("roundNumber", roundNumber)
                .bind("position", index + 1)
                .bind("userId", userId)
                .execute()
        }
    }

    override fun getRoundWinner(gameId: Int): RoundWinnerInfo {
        val roundNumber =
            getCurrentRoundNumber(gameId)
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
                            name = Name(rs.getString("username")),
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
        val roundNumber = getCurrentRoundNumber(gameId) ?: return 0

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
               COALESCE(t.score, 0) AS score
        FROM match_players mp
        JOIN users u ON mp.user_id = u.id
        LEFT JOIN rounds r ON r.match_id = mp.match_id
        LEFT JOIN turn t ON t.user_id = u.id 
            AND t.match_id = mp.match_id 
            AND t.round_number = r.round_number
        WHERE mp.match_id = :gameId
          AND r.round_number = (
              SELECT MAX(round_number) 
              FROM rounds 
              WHERE match_id = :gameId
          )
        GROUP BY u.id, u.username, u.nick_name, u.email, u.avatar_url, u.balance, t.score
        """,
            ).bind("gameId", gameId)
            .map(PlayerScoreMapper())
            .list()
            .let { pointPlayers ->
                val queue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })
                queue.addAll(pointPlayers)
                Scoreboard(queue)
            }

    override fun getCurrentRoundNumber(gameId: Int): Int? =
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

    override fun getTotalRoundsOfGame(gameId: Int): Int =
        handle
            .createQuery(
                """
                select total_rounds from matches m where m.id = :gameId
                """.trimIndent(),
            ).bind("gameId", gameId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)

    override fun populateEmptyTurns(
        matchId: Int,
        roundNumber: Int,
    ) {
        val players =
            handle
                .createQuery(
                    """
        SELECT user_id
        FROM match_players
        WHERE match_id = :matchId
        """,
                ).bind("matchId", matchId)
                .mapTo(Int::class.java)
                .list()

        players.forEach { userId ->
            handle
                .createUpdate(
                    """
            INSERT INTO turn (match_id, round_number, user_id, hand, roll_number, score)
            VALUES (:matchId, :roundNumber, :userId, NULL, 0, NULL)
            ON CONFLICT (match_id, round_number, user_id) DO NOTHING
            """,
                ).bind("matchId", matchId)
                .bind("roundNumber", roundNumber)
                .bind("userId", userId)
                .execute()
        }
    }

    override fun getCurrentPlayerTurn(gameId: Int): Int {
        val roundNumber = getCurrentRoundNumber(gameId) ?: throw IllegalStateException("No rounds found for game $gameId")

        val order = getRoundOrder(gameId)
        if (order.isEmpty()) throw IllegalStateException("No players found for game $gameId")

        // Find the first player who has not rolled yet (roll_number = 0)
        order.forEach { userId ->
            val rollNumber =
                handle
                    .createQuery(
                        """
            SELECT COALESCE(roll_number, 0) 
            FROM turn
            WHERE match_id = :gameId
              AND round_number = :roundNumber
              AND user_id = :userId
            """,
                    ).bind("gameId", gameId)
                    .bind("roundNumber", roundNumber)
                    .bind("userId", userId)
                    .mapTo(Int::class.java)
                    .findOne()
                    .orElse(0)

            if (rollNumber == 0) return userId
        }
        return order.first()
    }
}
