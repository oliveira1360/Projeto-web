package org.example.controllers

import org.example.TransactionManagerMem
import org.example.config.LobbiesDomainConfig
import org.example.config.UsersDomainConfig
import org.example.game.RepositoryGameMem
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.token.Sha256TokenEncoder
import org.example.user.RepositoryUserMem
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Clock
import java.time.Duration

@TestConfiguration
@ComponentScan("org.example")
@EnableWebMvc
open class TestConfig : WebMvcConfigurer {
    private val userMem: RepositoryUserMem
        get() = RepositoryLobbyMem.userRepo
    private val lobbyMem = RepositoryLobbyMem()
    private val gameMem = RepositoryGameMem()
    private var generalMem: RepositoryInviteMem = RepositoryInviteMem()

    @Bean
    open fun trxManagerMem(): TransactionManagerMem =
        TransactionManagerMem(
            userMem,
            lobbyMem,
            gameMem,
            generalMem,
        )

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

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Não adicionar interceptors nos testes para evitar problemas de autenticação
        // Os testes podem mockar a autenticação diretamente
    }
}
