package org.example

interface Transaction {
    val repositoryUser: RepositoryUser
    val repositoryLobby: RepositoryLobby

    fun rollback()
}