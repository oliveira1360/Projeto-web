package org.example

import org.example.game.RepositoryGame
import org.example.lobby.RepositoryLobby
import org.example.user.RepositoryUser

class TransactionManagerMem(
    repositoryUser: RepositoryUser,
    repositoryLobby: RepositoryLobby,
    repositoryGame: RepositoryGame,
) : TransactionManager {
    private val transaction = TransactionMem(repositoryUser, repositoryLobby, repositoryGame)

    override fun <R> run(block: Transaction.() -> R): R = block(transaction)
}
