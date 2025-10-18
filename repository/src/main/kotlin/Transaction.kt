package org.example

import org.example.game.RepositoryGame
import org.example.general.RepositoryGeneral
import org.example.lobby.RepositoryLobby
import org.example.user.RepositoryUser

interface Transaction {
    val repositoryUser: RepositoryUser
    val repositoryLobby: RepositoryLobby
    val repositoryGame: RepositoryGame
    val repositoryGeneral: RepositoryGeneral

    fun rollback()
}
