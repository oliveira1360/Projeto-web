package mock

import org.example.RepositoryLobby
import org.example.RepositoryUser
import org.example.Transaction
import org.example.TransactionManager

class TransactionManagerMem(
    val repositoryUser: RepositoryUser,
    val repositoryLobby: RepositoryLobby
): TransactionManager {
    private val transaction = TransactionMem(repositoryUser, repositoryLobby)

    override fun <R> run(block: Transaction.() -> R): R {
        return transaction.block()
    }
}
