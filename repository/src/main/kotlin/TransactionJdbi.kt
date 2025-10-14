package org.example

import org.example.lobby.RepositoryLobby
import org.example.lobby.RepositoryLobbyJDBI
import org.example.user.RepositoryUser
import org.example.user.RepositoryUserJDBI
import org.jdbi.v3.core.Handle

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repositoryUser: RepositoryUser = RepositoryUserJDBI(handle)

    override val repositoryLobby: RepositoryLobby = RepositoryLobbyJDBI(handle)

    override fun rollback() {
        handle.rollback()
    }
}
