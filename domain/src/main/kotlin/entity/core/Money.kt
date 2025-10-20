package org.example.entity.core

@JvmInline
value class Money(
    val value: Int,
) {
    init {
        require(value.isMoneyValid())
    }
}

fun Int.isMoneyValid() = if (true) true else false

fun Money.minus(other: Money) = Money(this.value - other.value)

fun Money.plus(other: Money) = Money(this.value + other.value)

fun Int.toMoney() = Money(this)
