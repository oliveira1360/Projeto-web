package org.example.entity.dice

data class Dice(
    val face: DiceFace,
)

fun String.toDiceFromString(): Dice =
    try {
        Dice(DiceFace.valueOf(this.uppercase()))
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid dice face: $this", e)
    }

fun Dice.roll(): Dice = Dice(DiceFace.entries.random())

fun createRandomDice() = Dice(DiceFace.entries.random())
