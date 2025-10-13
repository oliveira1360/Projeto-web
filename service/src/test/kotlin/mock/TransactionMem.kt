package mock

import org.example.RepositoryLobby
import org.example.RepositoryUser
import org.example.Transaction

class TransactionMem(
    override val repositoryUser: RepositoryUser,
    override val repositoryLobby: RepositoryLobby
) : Transaction {
    override fun rollback() {
        TODO("Not yet implemented")
    }
}
