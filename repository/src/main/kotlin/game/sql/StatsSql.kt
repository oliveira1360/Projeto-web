package org.example.game.sql

object StatsSql {
    val UPDATE_STATS_ROUND_WINNER =
        """
        INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
        VALUES (:userId, 0, 0, 0, :points, 0, 0)
        ON CONFLICT (user_id) 
        DO UPDATE SET
            total_points = player_stats.total_points + :points
        """.trimIndent()

    val UPDATE_STATS_ROUND_LOSER =
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
        """.trimIndent()

    val UPDATE_STATS_GAME_WINNER =
        """
        INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
        VALUES (:userId, 1, 1, 0, :points, 1, 1)
        ON CONFLICT (user_id) 
        DO UPDATE SET
            total_games = player_stats.total_games + 1,
            total_wins = player_stats.total_wins + 1,
            current_streak = player_stats.current_streak + 1,
            longest_win_streak = GREATEST(player_stats.longest_win_streak, player_stats.current_streak + 1)
        """.trimIndent()

    val UPDATE_STATS_GAME_LOSER =
        """
        INSERT INTO player_stats (user_id, total_games, total_wins, total_losses, total_points, longest_win_streak, current_streak)
        VALUES (:userId, 1, 0, 1, :points, 0, 0)
        ON CONFLICT (user_id) 
        DO UPDATE SET
            total_games = player_stats.total_games + 1,
            total_losses = player_stats.total_losses + 1,
            current_streak = 0
        """.trimIndent()
}
