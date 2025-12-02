package config

import java.time.Duration

class InviteDomainConfig (
    val tokenTtl: Duration,
    val tokenRollingTtl: Duration,
    val maxTokensPerUser: Int,
    ) {
        init {
            require(tokenTtl.isPositive)
            require(tokenRollingTtl.isPositive)
            require(maxTokensPerUser > 0)
        }
    }