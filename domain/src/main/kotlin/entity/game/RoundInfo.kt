package org.example.entity.game

import org.example.entity.player.PointPlayer
import java.util.PriorityQueue

data class RoundInfo(
    val round: Round,
    val totalRounds: Round,
    val pointsQueue: PriorityQueue<PointPlayer>,
    val roundOrder: List<Int>,
    val turn: Int,
)
