package org.example.entity

@JvmInline
value class Money(
    val value: Int,
) {
    init {
        require(this.value > 0)
    }
}

fun Int.toMoney() = Money(this)
