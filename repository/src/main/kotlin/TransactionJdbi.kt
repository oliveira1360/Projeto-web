package org.example

import org.jdbi.v3.core.Handle


class TransactionJdbi(
    private val handle: Handle
) : Transaction {
    override val repositoryUser: RepositoryUser = RepositoryUserJBDI(handle)

    override fun rollback() {
        handle.rollback()
    }
}