package org.example.entity

@JvmInline
value class Round(
    private val round: Int,
) {
    init {
        require(round.isRoundValid())
    }
}
fun Int.isRoundValid() = if (this > 0) true else false

fun Int.toRound() = Round(this)
