package org.example.game.sql

object GameSqlWrite {
    const val DELETE_BY_ID = "DELETE FROM matches WHERE id = :id"
    const val CLEAR_ALL = "DELETE FROM matches"
    const val CLOSE_GAME = "UPDATE matches SET status = 'FINISHED' WHERE id = :gameId"

    const val INSERT_MATCH = "INSERT INTO matches (winner_id, total_rounds, status) VALUES (NULL, :totalRounds, 'ACTIVE')"
    const val INSERT_MATCH_PLAYER = "INSERT INTO match_players (match_id, user_id, seat_number) VALUES (:matchId, :userId, :seatNumber)"

    val DEDUCT_BALANCE =
        """
        UPDATE users 
        SET balance = balance - :betAmount 
        WHERE id IN (
            SELECT user_id 
            FROM match_players 
            WHERE match_id = :matchId
        )
        """.trimIndent()

    val FINISH_MATCH_AND_SET_WINNER =
        """
        UPDATE matches
        SET winner_id = :winnerId,
            ended_at = CURRENT_TIMESTAMP,
            status = 'FINISHED'
        WHERE id = :gameId
        """.trimIndent()
}
