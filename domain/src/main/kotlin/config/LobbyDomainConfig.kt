@file:Suppress("ktlint:standard:filename")

package org.example.config

data class LobbiesDomainConfig(
    val maxPlayersPerLobby: Int,
    val maxLobbiesPerUser: Int,
    val minPlayersToStart: Int = 2,
    val lobbyTimeoutSeconds: Long = 60,
) {
    init {
        require(maxPlayersPerLobby > 0) { "maxPlayersPerLobby must be greater than 0" }
        require(maxLobbiesPerUser > 0) { "maxLobbiesPerUser must be greater than 0" }
        require(minPlayersToStart > 1) { "minPlayersToStart must be greater than 1" }
        require(lobbyTimeoutSeconds > 0) { "lobbyTimeoutSeconds must be greater than 0" }
    }
}
