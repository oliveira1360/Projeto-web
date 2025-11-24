package org.example.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.dto.inputDto.AuthenticatedUserDto
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthenticationInterceptor(
    private val authorizationHeaderProcessor: RequestTokenProcessor,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler is HandlerMethod &&
            handler.methodParameters.any {
                it.parameterType == AuthenticatedUserDto::class.java
            }
        ) {
            val token = extractTokenFromCookies(request)
            // enforce authentication
            val user =
                token?.let {
                    authorizationHeaderProcessor.processCookieToken(it)
                }

            return if (user == null) {
                response.status = 401
                response.addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME)
                false
            } else {
                AuthenticatedUserArgumentResolver.addUserTo(user, request)
                true
            }
        }

        return true
    }

    companion object {
        private const val NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate"
    }

    private fun extractTokenFromCookies(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == "token" }
            ?.value
}
