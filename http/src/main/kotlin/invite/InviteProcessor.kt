package org.example.invite

import org.example.InviteService
import org.example.dto.inputDto.ValidInviteDTO
import org.springframework.stereotype.Component

@Component
class InviteProcessor(
    val inviteService: InviteService,
) {
    fun processorInviteHeaderValue(authorizationValue: String?): ValidInviteDTO? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        return inviteService.getInvite(parts[1])?.let {
            ValidInviteDTO(it.id, it.token, it.createdAt, it.expiresAt, it.used)
        }
    }

    companion object {
        const val SCHEME = "invite"
    }
}
