package org.example

import jakarta.inject.Named
import org.example.entity.core.Invite
import java.time.Clock
import java.util.UUID

@Named
class InviteService(
    private val trxManager: TransactionManager,
    private val clock: Clock,
) {
    private val expirationTimeInSeconds = 10L
    fun getInvite(invite: String): Invite? =
        trxManager.run {
            val result = repositoryGeneral.getInvite(invite) ?: return@run null
            result
        }

    fun createInvite() : Either<UserError, Invite> =
        trxManager.run {
            val inviteCode = UUID.randomUUID().toString()
            val now = clock.instant()
            val invite = Invite(
                id = 0,
                token = inviteCode,
                createdAt = now,
                expiresAt = now.plusSeconds(expirationTimeInSeconds),
                used = false,
            )
            val result = repositoryGeneral.createInvite(invite) ?: return@run Failure(UserError.InvalidCredentials)
            Success(result)

        }
}
