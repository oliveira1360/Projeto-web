package org.example.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus

/**
 * RFC 7807 Problem Details for HTTP APIs
 * https://tools.ietf.org/html/rfc7807
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProblemDetail(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val additionalProperties: Map<String, Any>? = null,
)

/**
 * Problem type URIs for game-related errors
 */
object ProblemTypes {
    const val BASE_URI = "https://api.pokerdice.com/problems"

    // Game errors
    const val EMPTY_HAND = "$BASE_URI/empty-hand"
    const val NO_ROUND_IN_PROGRESS = "$BASE_URI/no-round-in-progress"
    const val GAME_NOT_FINISHED = "$BASE_URI/game-not-finished"
    const val GAME_NOT_FOUND = "$BASE_URI/game-not-found"
    const val INVALID_GAME_ID = "$BASE_URI/invalid-game-id"
    const val TOO_MANY_ROLLS = "$BASE_URI/too-many-rolls"
    const val ROUND_NOT_STARTED = "$BASE_URI/round-not-started"
    const val GAME_ALREADY_FINISHED = "$BASE_URI/game-already-finished"
    const val NO_PLAYERS_IN_GAME = "$BASE_URI/no-players-in-game"
    const val INVALID_DICE_INDICES = "$BASE_URI/invalid-dice-indices"
    const val ALL_PLAYERS_NOT_FINISHED = "$BASE_URI/all-players-not-finished"

    // Lobby errors
    const val LOBBY_NOT_FOUND = "$BASE_URI/lobby-not-found"
    const val LOBBY_FULL = "$BASE_URI/lobby-full"
    const val ALREADY_IN_LOBBY = "$BASE_URI/already-in-lobby"
    const val NOT_IN_LOBBY = "$BASE_URI/not-in-lobby"

    // User/Auth errors
    const val USER_NOT_FOUND = "$BASE_URI/user-not-found"
    const val USER_NOT_IN_GAME = "$BASE_URI/user-not-in-game"
    const val UNAUTHORIZED_ACTION = "$BASE_URI/unauthorized-action"
    const val INVALID_CREDENTIALS = "$BASE_URI/invalid-credentials"
    const val INSECURE_PASSWORD = "$BASE_URI/insecure-password"
    const val EMAIL_ALREADY_USED = "$BASE_URI/email-already-used"
    const val INVALID_USER_ID = "$BASE_URI/invalid-user-id"

    // Authorization
    const val UNAUTHORIZED = "$BASE_URI/unauthorized"
    const val FORBIDDEN = "$BASE_URI/forbidden"
}

/**
 * Helper function to create ProblemDetail instances
 */
fun createProblemDetail(
    type: String,
    title: String,
    status: HttpStatus,
    detail: String? = null,
    instance: String? = null,
    additionalProperties: Map<String, Any>? = null,
): ProblemDetail =
    ProblemDetail(
        type = type,
        title = title,
        status = status.value(),
        detail = detail,
        instance = instance,
        additionalProperties = additionalProperties,
    )
