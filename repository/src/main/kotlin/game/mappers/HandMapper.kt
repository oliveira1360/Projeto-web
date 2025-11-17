package org.example.game.mappers

import org.example.entity.dice.toDiceFromString
import org.example.entity.player.Hand
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class HandMapper : RowMapper<Hand> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Hand {
        val pgArray = rs.getArray("hand")
        if (pgArray == null) {
            return Hand(emptyList())
        }
        val handArray = pgArray.array as Array<*>
        val diceList = handArray.mapNotNull { it?.toString()?.toDiceFromString() }
        return Hand(diceList)
    }
}