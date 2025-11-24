package org.example.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TokenCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getAttribute("authToken") as? String
        if (token != null) {
            response.addHeader(
                HttpHeaders.SET_COOKIE,
                "token=$token; HttpOnly; Path=/; Max-Age=86400",
            )
        }
        filterChain.doFilter(request, response)
    }
}
