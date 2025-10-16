package org.example.entity.core

@JvmInline
value class Quantity(
    val value: Int,
) {
    init {
        require(value >= 0)
    }
}

fun Int.toQuantity() = Quantity(this)
