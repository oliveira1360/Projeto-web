package org.example.game.mappers

import org.example.entity.core.Points
import org.example.entity.player.PointPlayer
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class PlayerScoreMapper : RowMapper<PointPlayer> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): PointPlayer =
        PointPlayer(
            player = PlayerGameInfoMapper().map(rs, ctx),
            points = Points(rs.getInt("score")),
        )
}