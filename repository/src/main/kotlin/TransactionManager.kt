package org.example

interface TransactionManager {
    /**
     * This method creates an instance of Transaction, potentially
     * initializing a JDBC Connection,a JDBI Handle, or another resource,
     * which is then passed as an argument to the Transaction constructor.
     */
    fun <R> run(block: Transaction.() -> R): R
}