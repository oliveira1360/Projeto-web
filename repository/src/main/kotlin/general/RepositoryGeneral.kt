package org.example.general

import org.example.entity.core.Invite

interface RepositoryGeneral {
    fun getInvite(invite: String): Invite?

    fun createInvite(invite: Invite): Invite?
}
