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
    const val LOBBIES = "lobbies"
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
    const val CREATE = "create"
    const val LOGIN = "login"
    const val LOGOUT = "logout"
    const val UPDATE = "update"
    const val STATS = "stats"
    const val INFO = "info"
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

object LobbyLinks {
    fun createLobby(lobbyId: Int) = mapOf(
        "self" to mapOf("href" to "/lobbies/$lobbyId"),
        "list" to mapOf("href" to "/lobbies"),
        "join" to mapOf("href" to "/lobbies/join/$lobbyId"),
        "details" to mapOf("href" to "/lobbies/$lobbyId")
    )

    fun listLobbies() = mapOf(
        "self" to mapOf("href" to "/lobbies"),
        "create" to mapOf("href" to "/lobbies/create")
    )

    fun lobbyDetails(lobbyId: Int) = mapOf(
        "self" to mapOf("href" to "/lobbies/$lobbyId"),
        "list" to mapOf("href" to "/lobbies"),
        "join" to mapOf("href" to "/lobbies/join/$lobbyId"),
        "leave" to mapOf("href" to "/lobbies/leave/$lobbyId")
    )

    fun joinLobby(lobbyId: Int) = mapOf(
        "self" to mapOf("href" to "/lobbies/join/$lobbyId"),
        "details" to mapOf("href" to "/lobbies/$lobbyId"),
        "leave" to mapOf("href" to "/lobbies/leave/$lobbyId"),
        "list" to mapOf("href" to "/lobbies")
    )

    fun leaveLobby() = mapOf(
        "list" to mapOf("href" to "/lobbies"),
        "create" to mapOf("href" to "/lobbies/create")
    )
}



object UserLinks {
    fun createUser(userId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/user/info"),
            LinkRelations.LOGIN to mapOf("href" to "/user/login", "method" to "POST"),
            LinkRelations.UPDATE to mapOf("href" to "/user/update", "method" to "POST"),
            LinkRelations.STATS to mapOf("href" to "/user/stats"),
        )

    fun login() =
        mapOf(
            LinkRelations.INFO to mapOf("href" to "/user/info"),
            LinkRelations.LOGOUT to mapOf("href" to "/user/logout", "method" to "POST"),
            LinkRelations.STATS to mapOf("href" to "/user/stats"),
        )

    fun userInfo(userId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/user/info"),
            LinkRelations.UPDATE to mapOf("href" to "/user/update", "method" to "POST"),
            LinkRelations.STATS to mapOf("href" to "/user/stats"),
            LinkRelations.LOGOUT to mapOf("href" to "/user/logout", "method" to "POST"),
        )

    fun updateUser(userId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/user/info"),
            LinkRelations.INFO to mapOf("href" to "/user/info"),
            LinkRelations.STATS to mapOf("href" to "/user/stats"),
        )

    fun logout() =
        mapOf(
            LinkRelations.LOGIN to mapOf("href" to "/user/login", "method" to "POST"),
            LinkRelations.CREATE to mapOf("href" to "/user/create", "method" to "POST"),
        )

    fun userStats(userId: Int) =
        mapOf(
            LinkRelations.SELF to mapOf("href" to "/user/stats"),
            LinkRelations.INFO to mapOf("href" to "/user/info"),
        )
}