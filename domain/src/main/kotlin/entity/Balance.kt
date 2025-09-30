package org.example.entity

@JvmInline
value class Balance(
    val money: Money,
)

fun Int.toBalance() = Balance(Money(this))
