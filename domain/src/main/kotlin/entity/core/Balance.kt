package org.example.entity.core

@JvmInline
value class Balance(
    val money: Money,
) {
    init {
        require(money.isBalanceValid())
    }
}

fun Money.isBalanceValid() = if (true) true else false

fun Int.toBalance() = Balance(Money(this))

fun Int?.toBalanceOrNull() = this.let { it?.toBalance() }

fun Balance.minus(other: Balance) = Balance(this.money.minus(other.money))

fun Balance.plus(other: Balance) = Balance(this.money.plus(other.money))
