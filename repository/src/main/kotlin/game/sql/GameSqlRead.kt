package org.example.game.sql

object GameSqlRead {
    val FIND_BY_ID =
        """
        SELECT id, total_rounds, status, winner_id 
        FROM matches 
        WHERE id = :id
        """.trimIndent()

    val FIND_ALL =
        """
         SELECT id, total_rounds, status, winner_id 
        FROM matches
        """.trimIndent()

    const val FIND_PLAYERS_BY_MATCH = """
        SELECT u.id, u.username, u.nick_name, u.password_hash, 
               u.email, u.avatar_url, u.balance
        FROM users u
        JOIN match_players mp ON u.id = mp.user_id
        WHERE mp.match_id = :matchId
        ORDER BY mp.seat_number
    """

    const val FIND_ROUNDS_BY_MATCH = """
        SELECT round_number
        FROM rounds
        WHERE match_id = :matchId
        ORDER BY round_number
    """

    const val FIND_ROUND_SCORES = """
        SELECT user_id, score
        FROM turn
        WHERE match_id = :matchId AND round_number = :roundNumber
    """

    const val LIST_PLAYERS_IN_GAME = """
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
            """

    const val GET_SCORES = """
        SELECT u.id AS user_id,
               u.username,
               u.nick_name,
               u.email,
               u.avatar_url,
               u.balance,
               0 AS roll_number,
               NULL AS hand,
               t.score 
        FROM match_players mp
        JOIN users u ON mp.user_id = u.id
        JOIN rounds r ON r.match_id = mp.match_id  
        JOIN turn t ON t.user_id = u.id           
            AND t.match_id = mp.match_id 
            AND t.round_number = r.round_number
            AND t.finished = TRUE
        WHERE mp.match_id = :gameId
          AND r.round_number = (
              SELECT MAX(round_number) 
              FROM rounds 
              WHERE match_id = :gameId
          )
    """

    const val GET_LOBBY_ROUNDS = "SELECT rounds FROM lobbies WHERE id = :lobbyId"

    val GET_LOBBY_PLAYERS =
        """
        SELECT user_id 
        FROM lobby_players 
        WHERE lobby_id = :lobbyId
        ORDER BY joined_at
        """.trimIndent()

    const val REMAINING_TIME = """
            SELECT EXTRACT(EPOCH FROM (started_at + INTERVAL '30 seconds' - CURRENT_TIMESTAMP)) AS remaining
            FROM matches WHERE id = :gameId
            """

    const val GET_TOTAL_ROUNDS = "select total_rounds from matches m where m.id = :gameId"

    const val GET_MATCH_PLAYERS_SIMPLE = """
        SELECT user_id
        FROM match_players
        WHERE match_id = :matchId
        """

    const val GET_PLAYERS_BY_SEAT = """
            SELECT user_id 
            FROM match_players
            WHERE match_id = :gameId
            ORDER BY seat_number
            """

    val FIND_GAME_WINNER =
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
        """.trimIndent()

    val GET_FINAL_SCORES =
        """
        SELECT 
            mp.user_id,
            COALESCE(SUM(t.score), 0) AS total_score,
            CASE WHEN mp.user_id = :winnerId THEN 1 ELSE 0 END AS is_winner
        FROM match_players mp
        LEFT JOIN turn t ON t.user_id = mp.user_id AND t.match_id = mp.match_id
        WHERE mp.match_id = :gameId
        GROUP BY mp.user_id
        """.trimIndent()

    val GET_GAME_STATUS =
        """
        SELECT status 
        FROM matches 
        WHERE id = :gameId
        """.trimIndent()

    val IS_GAME_ACTIVE =
        """
        SELECT CASE 
            WHEN status = 'ACTIVE' THEN TRUE 
            ELSE FALSE 
        END AS is_active
        FROM matches 
        WHERE id = :gameId
        """.trimIndent()
}
