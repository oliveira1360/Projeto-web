package org.example.entity.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Money
import org.example.entity.player.User
import java.util.PriorityQueue

data class Game(
    val playersGameInfoList: List<User>,
    val rounds: List<RoundInfo>,
    val pot: Money,
)

data class RoundInfo(
    val round: Round,
    val points: PriorityQueue<Pair<Int, PlayerGameInfo>>,
)
