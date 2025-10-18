package org.example.controllers

object ApiMediaTypes {
    // Standard media types
    const val APPLICATION_JSON = "application/json"
    const val APPLICATION_PROBLEM_JSON = "application/problem+json"
}

object LinkRelations {
    // Standard IANA link relations
    const val SELF = "self"

    // Custom link relations
    const val GAME = "game"
    const val START_GAME = "start-game"
    const val LOBBY = "lobby"
    const val PLAYER = "player"
    const val PLAYERS = "players"
    const val ROUND = "round"
    const val SHUFFLE = "shuffle"
    const val FINISH_TURN = "finish-turn"
    const val SCOREBOARD = "scoreboard"
    const val WINNER = "winner"
    const val JOIN = "join"
    const val LEAVE = "leave"
    const val CLOSE = "close"
    const val FINISH = "finish"
    const val HAND = "hand"
}

object GameLinks {
    fun createGame(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId"),
            LinkRelations.START_GAME to mapOf("href" to "/game/$gameId/round/start", "method" to "POST"),
            LinkRelations.PLAYER to mapOf("href" to "/game/$gameId/players"),
            LinkRelations.CLOSE to mapOf("href" to "/game/$gameId", "method" to "DELETE"),
        )

    fun listPlayersInGame(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/players"),
            LinkRelations.GAME to mapOf("href" to "/game/$gameId"),
        )

    fun startRound(gameId: Int) =
        mapOf(
            LinkRelations.GAME to mapOf("href" to "/game/$gameId"),
            LinkRelations.ROUND to mapOf("href" to "/game/$gameId/round"),
            LinkRelations.SHUFFLE to mapOf("href" to "/game/$gameId/player/shuffle", "method" to "POST"),
            LinkRelations.HAND to mapOf("href" to "/game/$gameId/player/hand"),
        )

    fun playerHand(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/player/hand"),
            LinkRelations.SHUFFLE to mapOf("href" to "/game/$gameId/player/shuffle", "method" to "POST"),
            LinkRelations.FINISH to mapOf("href" to "/game/$gameId/player/finish", "method" to "PUT"),
        )

    fun shuffle(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/player/hand"),
            LinkRelations.SHUFFLE to mapOf("href" to "/game/$gameId/player/shuffle", "method" to "POST"),
            LinkRelations.FINISH to mapOf("href" to "/game/$gameId/player/finish", "method" to "PUT"),
        )

    fun finishTurn(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/player"),
            LinkRelations.ROUND to mapOf("href" to "/game/$gameId/round"),
            LinkRelations.SCOREBOARD to mapOf("href" to "/game/$gameId/scores"),
        )

    fun getRoundWinner(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/round/winner"),
            LinkRelations.ROUND to mapOf("href" to "/game/$gameId/round"),
            LinkRelations.SCOREBOARD to mapOf("href" to "/game/$gameId/scores"),
        )

    fun getGameWinner(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/winner"),
            LinkRelations.GAME to mapOf("href" to "/game/$gameId"),
            LinkRelations.SCOREBOARD to mapOf("href" to "/game/$gameId/scores"),
        )

    fun remainingTime(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/remaining-time"),
            LinkRelations.GAME to mapOf("href" to "/game/$gameId"),
        )

    fun getRoundInfo(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/round"),
            LinkRelations.GAME to mapOf("href" to "/game/$gameId"),
            LinkRelations.WINNER to mapOf("href" to "/game/$gameId/round/winner"),
        )

    fun getScoreboard(gameId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/game/$gameId/scores"),
            LinkRelations.GAME to mapOf("href" to "/game/$gameId"),
        )
}
