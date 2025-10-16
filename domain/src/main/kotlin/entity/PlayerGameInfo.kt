package org.example.entity

import org.example.entity.core.Balance
import org.example.entity.core.Quantity
import org.example.entity.player.Hand

data class PlayerGameInfo(
    val playerId: Int,
    val rolls: Quantity = Quantity(3),
    val hands: Hand,
    val balance: Balance,
)
