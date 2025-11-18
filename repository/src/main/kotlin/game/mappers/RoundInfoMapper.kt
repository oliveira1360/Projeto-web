package org.example.game.mappers

import org.example.entity.game.RoundInfo
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class RoundInfoMapper : RowMapper<RoundInfo> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): RoundInfo {
        TODO()
    }
}
