package org.example

import config.InviteDomainConfig
import jakarta.inject.Named
import org.example.entity.core.Invite
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Named
class InviteService(
    private val trxManager: TransactionManager,
    private val clock: Clock,
    private val config: InviteDomainConfig,
) {
    private val expirationTimeInSeconds = (60 * 5).toLong()

    fun getInvite(invite: String): Invite? =
        trxManager.run {
            val result = repositoryGeneral.getInvite(invite) ?: return@run null
            if (!isInviteTimeValid(clock, result)) {
                return@run null
            }
            result
        }

    fun createInvite(): Either<UserError, Invite> =
        trxManager.run {
            val inviteCode = UUID.randomUUID().toString()
            val now = clock.instant()
            val invite =
                Invite(
                    id = 0,
                    token = inviteCode,
                    createdAt = now,
                    expiresAt = now.plusSeconds(expirationTimeInSeconds),
                    used = false,
                )
            val result = repositoryGeneral.createInvite(invite) ?: return@run Failure(UserError.InvalidCredentials)
            Success(result)
        }

    private fun isInviteTimeValid(
        clock: Clock,
        invite: Invite,
    ): Boolean {
        val now = clock.instant()
        return now >= invite.createdAt && now <= invite.expiresAt
    }
}
