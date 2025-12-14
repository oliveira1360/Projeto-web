package org.example.mapper

import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Money
import org.example.entity.core.Name
import org.example.entity.core.URL
import org.example.entity.core.toPasswordFromHash
import org.example.entity.player.User
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class UserMapper : RowMapper<User> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): User =
        User(
            id = rs.getInt("id"),
            name = Name(rs.getString("username")),
            nickName = Name(rs.getString("nick_name")),
            email = Email(rs.getString("email")),
            imageUrl = rs.getString("avatar_url")?.let { URL(it) },
            passwordHash = rs.getString("password_hash").toPasswordFromHash(),
            balance = Balance(Money(rs.getInt("balance"))),
        )
}
