package org.example

import org.example.lobby.RepositoryLobby
import org.example.user.RepositoryUser

interface Transaction {
    val repositoryUser: RepositoryUser
    val repositoryLobby: RepositoryLobby

    fun rollback()
}