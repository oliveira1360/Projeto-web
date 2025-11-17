package org.example.game.mappers

import org.example.entity.game.Round
import org.example.entity.game.RoundInfo
import org.example.entity.player.PointPlayer
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.util.PriorityQueue

class RoundInfoMapper : RowMapper<RoundInfo> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): RoundInfo {
        TODO()
    }
}