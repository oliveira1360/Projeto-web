package org.example

import jakarta.inject.Named
import org.example.entity.core.Invite

@Named
class InviteService(
    private val trxManager: TransactionManager,
) {
    fun getInvite(invite: String): Invite? =
        trxManager.run {
            val result = repositoryGeneral.getInvite(invite) ?: return@run null
            result
        }
}
