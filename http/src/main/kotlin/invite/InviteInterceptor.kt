package org.example.invite

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.dto.inputDto.ValidInviteDTO
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class InviteInterceptor(
    private val inviteProcessor: InviteProcessor,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler is HandlerMethod &&
            handler.methodParameters.any {
                it.parameterType == ValidInviteDTO::class.java
            }
        ) {
            val invite = inviteProcessor.processorInviteHeaderValue(request.getHeader(NAME_AUTHORIZATION_HEADER))
            return if (invite == null) {
                response.status = 403
                response.contentType = "application/problem+json"
                response.writer.write(
                    """
                    {
                      "type": "https://github.com/isel-leic-daw/2025-daw-leic52d-2025-leic52d-14/tree/main/docs/problemTypes/invalid-invite.kt",
                      "title": "Invalid Invite Code",
                      "status": 403,
                      "detail": "The provided invite code is invalid or expired.",
                      "instance": "${request.requestURI}",
                      "timestamp": ${System.currentTimeMillis()}
                    }
                    """.trimIndent(),
                )
                false
            } else {
                InviteArgumentResolver.addInviteTo(invite, request)
                true
            }
        }
        return true
    }

    companion object {
        const val NAME_AUTHORIZATION_HEADER = "Authorization"
        private const val NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate"
    }
}
