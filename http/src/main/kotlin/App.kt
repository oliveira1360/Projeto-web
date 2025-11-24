@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example

import org.example.auth.*
import org.example.config.GameDomainConfig
import org.example.config.LobbiesDomainConfig
import org.example.config.UsersDomainConfig
import org.example.filter.TokenCookieFilter
import org.example.invite.InviteArgumentResolver
import org.example.invite.InviteInterceptor
import org.example.token.Sha256TokenEncoder
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Clock
import java.time.Duration

@Configuration
open class PipelineConfigurer(
    private val authenticationInterceptor: AuthenticationInterceptor,
    private val inviteInterceptor: InviteInterceptor,
    private val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
    private val inviteArgumentResolver: InviteArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authenticationInterceptor)
        registry.addInterceptor(inviteInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
        resolvers.add(inviteArgumentResolver)
    }
}

@SpringBootApplication
open class App {
    @Bean
    open fun jdbi() =
        Jdbi
            .create(
                PGSimpleDataSource().apply {
                    setURL(Environment.getDbUrl())
                },
            ).configureWithAppRequirements()

    @Bean
    open fun trxManagerJdbi(jdbi: Jdbi): TransactionManagerJdbi = TransactionManagerJdbi(jdbi)

    @Bean
    open fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    open fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    open fun clock(): Clock = Clock.systemUTC()

    @Bean
    open fun usersDomainConfig() =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
        )

    @Bean
    open fun lobbiesDomainConfig() =
        LobbiesDomainConfig(
            maxPlayersPerLobby = 6,
            maxLobbiesPerUser = 1,
        )

    @Bean
    open fun gameDomainConfig() =
        GameDomainConfig(
            moneyRemove = 1,
        )

    @Bean
    open fun tokenCookieFilter() = TokenCookieFilter()
}

fun main() {
    runApplication<App>()
}
