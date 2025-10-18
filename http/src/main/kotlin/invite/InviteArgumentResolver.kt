package org.example.invite

import jakarta.servlet.http.HttpServletRequest
import org.example.dto.inputDto.ValidInviteDTO
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class InviteArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.parameterType == ValidInviteDTO::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw IllegalStateException("TODO")

        return getInviteFrom(request)
            ?: throw IllegalStateException("TODO")
    }

    companion object {
        private const val KEY = "InviteArgumentResolver"

        fun addInviteTo(
            invite: ValidInviteDTO,
            request: HttpServletRequest,
        ) = request.setAttribute(KEY, invite)

        fun getInviteFrom(request: HttpServletRequest): ValidInviteDTO? = request.getAttribute(KEY) as? ValidInviteDTO
    }
}
