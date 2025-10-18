package org.example

import org.example.game.RepositoryGame
import org.example.general.RepositoryGeneral
import org.example.lobby.RepositoryLobby
import org.example.user.RepositoryUser

class TransactionManagerMem(
    repositoryUser: RepositoryUser,
    repositoryLobby: RepositoryLobby,
    repositoryGame: RepositoryGame,
    repositoryGeneral: RepositoryGeneral,
) : TransactionManager {
    private val transaction = TransactionMem(repositoryUser, repositoryLobby, repositoryGame, repositoryGeneral)

    override fun <R> run(block: Transaction.() -> R): R = block(transaction)
}
