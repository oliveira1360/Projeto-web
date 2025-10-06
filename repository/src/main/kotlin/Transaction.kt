package org.example

interface Transaction {
    val repositoryUser: RepositoryUser

    fun rollback()
}