package org.example.game.mappers

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Balance
import org.example.entity.core.Money
import org.example.entity.core.Name
import org.example.entity.core.URL
import org.example.entity.core.toQuantity
import org.example.entity.dice.toDiceFromString
import org.example.entity.player.Hand
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class PlayerGameInfoMapper : RowMapper<PlayerGameInfo> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): PlayerGameInfo {
        val playerId = rs.getInt("user_id")
        val name = rs.getString("username")
        val roll = rs.getInt("roll_number")
        val balance = Balance(Money(rs.getInt("balance")))
        val pgArray = rs.getArray("hand")
        val avatarUrl = rs.getString("avatar_url")
        val url = if (rs.wasNull()) null else URL(avatarUrl)
        val diceList =
            if (pgArray != null) {
                val handArray = pgArray.array as Array<*>
                handArray.mapNotNull {
                    try {
                        it?.toString()?.toDiceFromString()
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                emptyList()
            }

        return PlayerGameInfo(
            playerId = playerId,
            name = Name(name),
            rolls = roll.toQuantity(),
            hand = Hand(diceList),
            balance = balance,
            url = url,
        )
    }
}
