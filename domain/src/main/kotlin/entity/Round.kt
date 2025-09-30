package org.example.entity

@JvmInline
value class Round(
    private val round: Int,
) {
    init {
        require(round > 0)
    }
}

fun Int.toRound() = Round(this)
