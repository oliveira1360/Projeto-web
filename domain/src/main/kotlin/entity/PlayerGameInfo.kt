package org.example.entity

import org.example.entity.core.Balance
import org.example.entity.core.Name
import org.example.entity.core.Quantity
import org.example.entity.core.URL
import org.example.entity.player.Hand

data class PlayerGameInfo(
    val playerId: Int,
    val name: Name,
    val rolls: Quantity = Quantity(3),
    val hand: Hand,
    val balance: Balance,
    val url: URL? = null,
)
