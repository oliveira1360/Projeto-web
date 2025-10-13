@file:Suppress("ktlint:standard:filename")

package org.example

import org.example.entity.Lobby
import org.example.entity.User
import org.example.mapper.InstantMapper
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import java.time.Instant

fun Jdbi.configureWithAppRequirements(): Jdbi {
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())
    registerRowMapper(KotlinMapper(User::class))
    registerRowMapper(KotlinMapper(Lobby::class))
    registerColumnMapper(Instant::class.java, InstantMapper())
    return this
}
