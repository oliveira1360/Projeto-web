@file:Suppress("ktlint:standard:filename")

package org.example.config

data class LobbiesDomainConfig(
    val maxPlayersPerLobby: Int,
    val maxLobbiesPerUser: Int,
) {
    init {
        require(maxPlayersPerLobby > 0) { "maxPlayersPerLobby must be greater than 0" }
        require(maxLobbiesPerUser > 0) { "maxLobbiesPerUser must be greater than 0" }
    }
}
