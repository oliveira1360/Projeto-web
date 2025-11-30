package org.example.game.sql

object TurnSql {
    const val INIT_TURN = """
        INSERT INTO turn (match_id, round_number, user_id, hand, roll_number, score, finished)
        SELECT :gameId, :roundNumber, user_id, NULL, 0, NULL, FALSE
        FROM match_players
        WHERE match_id = :gameId
        ORDER BY seat_number
        """

    const val INSERT_EMPTY_TURN = """
                INSERT INTO turn (match_id, round_number, user_id, hand, roll_number, score, finished)
                VALUES (:matchId, :roundNumber, :userId, NULL, 0, NULL, FALSE)
                ON CONFLICT (match_id, round_number, user_id) DO NOTHING
            """

    const val GET_PLAYER_HAND = """
            SELECT hand FROM turn
            WHERE user_id = :userId 
            AND match_id = :gameId 
            AND round_number = :roundNumber
            AND hand IS NOT NULL
            """

    val GET_CURRENT_ROLL_NUMBER =
        """
        SELECT roll_number 
        FROM turn 
        WHERE match_id = :gameId 
          AND user_id = :userId 
          AND round_number = :roundNumber
        """.trimIndent()

    val UPDATE_HAND_AND_ROLL =
        """
        UPDATE turn
        SET 
            hand = :newHand::diceface[],
            roll_number = :newRollNumber
        WHERE match_id = :gameId
          AND user_id = :userId
          AND round_number = :roundNumber
        RETURNING hand, roll_number
        """.trimIndent()

    val UPDATE_SCORE =
        """
        UPDATE turn
        SET score = :score
        WHERE user_id = :userId 
          AND match_id = :gameId
          AND round_number = :roundNumber
        """.trimIndent()

    const val GET_ROLL_COUNT = """
                SELECT COALESCE(roll_number, 0) AS roll_count
                FROM turn
                WHERE match_id = :gameId
                  AND user_id = :userId
                  AND round_number = :roundNumber
                """

    const val GET_CURRENT_TURN = """
            SELECT ro.user_id
            FROM round_order ro
            JOIN turn t ON t.match_id = ro.match_id 
                AND t.round_number = ro.round_number 
                AND t.user_id = ro.user_id
            WHERE ro.match_id = :gameId
              AND ro.round_number = :roundNumber
              AND t.finished = FALSE 
            ORDER BY ro.order_position
            LIMIT 1
        """

    const val MARK_TURN_FINISHED = """
        UPDATE turn
        SET finished = TRUE
        WHERE match_id = :gameId
          AND round_number = :roundNumber
          AND user_id = :userId
        """

    const val GET_WINNER_SCORE = """
                SELECT score 
                FROM turn 
                WHERE match_id = :gameId 
                  AND round_number = :roundNumber 
                  AND user_id = :winnerId
                """
}
