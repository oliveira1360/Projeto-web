package org.example.entity.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Points
import org.example.entity.player.HandValues

data class RoundWinnerInfo(
    val player: PlayerGameInfo,
    val points: Points,
    val handValue: HandValues,
    val roundNumber: Int,
)
