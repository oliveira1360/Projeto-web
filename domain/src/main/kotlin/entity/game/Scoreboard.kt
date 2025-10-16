package org.example.entity.game

import org.example.entity.player.PointPlayer
import java.util.PriorityQueue

data class Scoreboard(
    val pointsQueue: PriorityQueue<PointPlayer>,
)
