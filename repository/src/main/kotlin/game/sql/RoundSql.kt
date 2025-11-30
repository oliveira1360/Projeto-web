package org.example.game.sql

object RoundSql {
    const val INSERT_ROUND = """
        INSERT INTO rounds (match_id, round_number)
        VALUES (:gameId, COALESCE(
            (SELECT MAX(round_number) FROM rounds WHERE match_id = :gameId), 0) + 1)
        RETURNING round_number
        """

    const val INSERT_ROUND_ORDER = """
            INSERT INTO round_order (match_id, round_number, order_position, user_id)
            VALUES (:gameId, :roundNumber, :position, :userId)
            """

    const val GET_CURRENT_ROUND_NUMBER = """
                SELECT MAX(round_number) 
                FROM rounds 
                WHERE match_id = :gameId
                """

    const val GET_LATEST_ROUND_INFO = """
            SELECT round_number 
            FROM rounds 
            WHERE match_id = :gameId 
            ORDER BY round_number DESC 
            LIMIT 1
            """

    const val GET_ROUND_ORDER = """
        SELECT user_id
        FROM round_order
        WHERE match_id = :gameId AND round_number = :roundNumber
        ORDER BY order_position
        """

    const val DELETE_ROUND_ORDER = """
        DELETE FROM round_order
        WHERE match_id = :gameId AND round_number = :roundNumber
        """

    val FIND_ROUND_WINNER_CANDIDATE =
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
           AND t.finished = TRUE
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
        """.trimIndent()

    const val SET_ROUND_WINNER = """
                UPDATE rounds
                SET winner_id = :winnerId
                WHERE match_id = :gameId AND round_number = :roundNumber
                """

    const val REWARD_WINNER = """
                UPDATE users 
                SET balance = balance + 2 
                WHERE id = :winnerId
                """
}
