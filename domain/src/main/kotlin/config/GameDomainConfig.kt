package org.example.config

class GameDomainConfig(
    val moneyRemove: Int,
) {
    init {
        require(moneyRemove >= 0)
    }
}
