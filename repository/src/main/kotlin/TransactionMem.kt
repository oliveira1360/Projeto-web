package org.example

import org.example.lobby.RepositoryLobby
import org.example.user.RepositoryUser

class TransactionMem(
    override val repositoryUser: RepositoryUser,
    override val repositoryLobby: RepositoryLobby,
) : Transaction {
    override fun rollback() {
        TODO("Not yet implemented")
    }
}
