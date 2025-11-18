package org.example.game.mappers

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Money
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.Points
import org.example.entity.core.URL
import org.example.entity.core.toQuantity
import org.example.entity.game.Game
import org.example.entity.game.Round
import org.example.entity.game.RoundInfo
import org.example.entity.player.Hand
import org.example.entity.player.PointPlayer
import org.example.entity.player.User
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.util.PriorityQueue

class GameMapper : RowMapper<Game> {
    override fun map(
        rs: ResultSet?,
        ctx: StatementContext?,
    ): Game? =
        Game(
            playersGameInfoList = rs?.getString("players_json")?.let { parsePlayers(it) } ?: emptyList(),
            rounds = rs?.getString("rounds_json")?.let { parseRounds(it) } ?: emptyList(),
        )

    private fun parsePlayers(json: String): List<User> {
        // JSON format: [{"id": 1, "name": "...", "nickName": "...", "email": "...", "imageUrl": "...", "balance": 1000}, ...]

        if (json == "[]" || json.isBlank()) return emptyList()

        return json
            .removeSurrounding("[", "]")
            .split("},")
            .mapNotNull { playerJson ->
                try {
                    val cleanJson = if (!playerJson.endsWith("}")) "$playerJson}" else playerJson

                    val id = extractJsonValue(cleanJson, "id")?.toIntOrNull() ?: return@mapNotNull null
                    val name = extractJsonValue(cleanJson, "name") ?: return@mapNotNull null
                    val nickName = extractJsonValue(cleanJson, "nickName") ?: return@mapNotNull null
                    val email = extractJsonValue(cleanJson, "email") ?: return@mapNotNull null
                    val imageUrl = extractJsonValue(cleanJson, "imageUrl")
                    val balance = extractJsonValue(cleanJson, "balance")?.toIntOrNull() ?: 0
                    val password = extractJsonValue(cleanJson, "password_hash") ?: ""

                    User(
                        id = id,
                        name = Name(name),
                        nickName = Name(nickName),
                        email = Email(email),
                        imageUrl = imageUrl?.let { URL(it) },
                        password = Password(password),
                        balance = Balance(Money(balance)),
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun parseRounds(json: String): List<RoundInfo> {
        // JSON format: [{"roundNumber": 1, "points": [{"playerId": 1, "score": 28}, ...]}, ...]

        if (json == "[]" || json.isBlank()) return emptyList()

        return json
            .removeSurrounding("[", "]")
            .split("},")
            .mapNotNull { roundJson ->
                try {
                    val cleanJson = if (!roundJson.endsWith("}")) "$roundJson}" else roundJson

                    val roundNumber =
                        extractJsonValue(cleanJson, "roundNumber")?.toIntOrNull()
                            ?: return@mapNotNull null

                    // Extract points array
                    val pointsJson = extractJsonArray(cleanJson, "points") ?: "[]"
                    val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

                    // Parse points for each player
                    if (pointsJson != "[]" && pointsJson != "null") {
                        pointsJson
                            .removeSurrounding("[", "]")
                            .split("},")
                            .forEach { pointJson ->
                                try {
                                    val cleanPointJson = if (!pointJson.endsWith("}")) "$pointJson}" else pointJson
                                    val playerId = extractJsonValue(cleanPointJson, "playerId")?.toIntOrNull()
                                    val score = extractJsonValue(cleanPointJson, "score")?.toIntOrNull()

                                    if (playerId != null && score != null) {
                                        pointsQueue.add(
                                            PointPlayer(
                                                player =
                                                    PlayerGameInfo(
                                                        playerId = playerId,
                                                        name = Name("empty"),
                                                        rolls = 0.toQuantity(),
                                                        hand = Hand(emptyList()),
                                                        balance = Balance(Money(0)),
                                                    ),
                                                points = Points(score),
                                            ),
                                        )
                                    }
                                } catch (e: Exception) {
                                    // Skip invalid point entries
                                }
                            }
                    }

                    RoundInfo(
                        round = Round(roundNumber),
                        pointsQueue = pointsQueue,
                        roundOrder = emptyList(),
                        turn = 0,
                        // todo
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun extractJsonValue(
        json: String,
        key: String,
    ): String? {
        val pattern = """"$key"\s*:\s*"?([^",}]+)"?""".toRegex()
        return pattern
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.trim()
    }

    private fun extractJsonArray(
        json: String,
        key: String,
    ): String? {
        val pattern = """"$key"\s*:\s*(\[.*?\])""".toRegex()
        return pattern
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.trim()
    }
}
