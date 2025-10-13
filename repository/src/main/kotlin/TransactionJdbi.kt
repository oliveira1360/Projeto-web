package org.example

import org.example.lobby.RepositoryLobby
import org.example.lobby.RepositoryLobbyJBDI
import org.example.user.RepositoryUser
import org.example.user.RepositoryUserJBDI
import org.jdbi.v3.core.Handle

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repositoryUser: RepositoryUser = RepositoryUserJBDI(handle)

    override val repositoryLobby: RepositoryLobby = RepositoryLobbyJBDI(handle)

    override fun rollback() {
        handle.rollback()
    }
}
