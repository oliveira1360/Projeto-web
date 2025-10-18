package org.example.general

import org.example.entity.core.Invite
import org.jdbi.v3.core.Handle

class RepositoryInviteJDBI(
    private val handle: Handle,
) : RepositoryGeneral {
    override fun getInvite(invite: String): Invite? =
        handle
            .createQuery(
                """
            SELECT id, invite_token, created_at, expires_at, used
            FROM invites
            WHERE invite_token = :invite
              AND used = FALSE
              AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
            """,
            ).bind("invite", invite)
            .map { rs, _ ->
                Invite(
                    id = rs.getInt("id"),
                    token = rs.getString("invite_token"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    expiresAt = rs.getTimestamp("expires_at")?.toInstant(),
                    used = rs.getBoolean("used"),
                )
            }.findOne()
            .orElse(null)

    override fun createInvite(invite: Invite): Invite? {
        handle
            .createUpdate(
                """
            INSERT INTO invites (invite_token, expires_at)
            VALUES (:token, :expiresAt)
            """,
            ).bind("token", invite.token)
            .bind("expiresAt", invite.expiresAt)
            .execute()
        return invite
    }
}
