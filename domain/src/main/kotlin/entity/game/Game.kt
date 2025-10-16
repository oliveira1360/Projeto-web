package org.example.entity.game

import org.example.entity.core.Money
import org.example.entity.player.User

data class Game(
    val playersGameInfoList: List<User>,
    val rounds: List<RoundInfo>,
    val pot: Money,
)
