package org.example.entity.player

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Points

data class PointPlayer(
    val player: PlayerGameInfo,
    val points: Points,
)
