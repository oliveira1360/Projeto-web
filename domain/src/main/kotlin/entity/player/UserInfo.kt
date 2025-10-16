package org.example.entity.player

data class UserInfo(
    val userId: Int,
    val totalGamesPlayed: Int,
    val totalWins: Int,
    val totalLosses: Int,
    val totalPoints: Int,
    val longestStreak: Int,
    val currentStreak: Int,
)
