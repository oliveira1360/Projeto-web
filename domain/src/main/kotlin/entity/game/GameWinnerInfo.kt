package org.example.entity.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Points

data class GameWinnerInfo(
    val player: PlayerGameInfo,
    val totalPoints: Points,
    val roundsWon: Int,
)
