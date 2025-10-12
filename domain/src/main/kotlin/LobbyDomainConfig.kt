package org.example

data class LobbiesDomainConfig(
    val maxPlayersPerLobby: Int,
    val inviteCodeLength: Int,
    val maxLobbiesPerUser: Int,
) {
    init {
        require(maxPlayersPerLobby > 0) { "maxPlayersPerLobby must be greater than 0" }
        require(inviteCodeLength in 4..12) { "inviteCodeLength must be between 4 and 12" }
        require(maxLobbiesPerUser > 0) { "maxLobbiesPerUser must be greater than 0" }
    }
}