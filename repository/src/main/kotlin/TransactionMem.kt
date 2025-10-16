package org.example

import org.example.game.RepositoryGame
import org.example.lobby.RepositoryLobby
import org.example.user.RepositoryUser

class TransactionMem(
    override val repositoryUser: RepositoryUser,
    override val repositoryLobby: RepositoryLobby,
    override val repositoryGame: RepositoryGame,
) : Transaction {
    override fun rollback() {
        TODO("Not yet implemented")
    }
}
