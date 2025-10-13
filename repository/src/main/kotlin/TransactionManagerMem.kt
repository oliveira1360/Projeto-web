package org.example

import org.example.lobby.RepositoryLobby
import org.example.user.RepositoryUser

class TransactionManagerMem(
    repositoryUser: RepositoryUser,
    repositoryLobby: RepositoryLobby,
) : TransactionManager {
    private val transaction = TransactionMem(repositoryUser, repositoryLobby)

    override fun <R> run(block: Transaction.() -> R): R = block(transaction)
}
