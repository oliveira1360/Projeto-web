package org.example.invite

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.auth.RequestTokenProcessor
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
                response.status = 401
                response.addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME)
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
