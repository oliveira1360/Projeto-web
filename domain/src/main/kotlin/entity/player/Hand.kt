package org.example.entity.player

import org.example.entity.dice.Dice

data class Hand(
    val value: List<Dice>,
) {
    fun evaluateHandValue(): HandValues {
        if (value.isEmpty()) return HandValues.NO_VALUE

        val faceCounts = value.groupingBy { it.face }.eachCount()
        val counts = faceCounts.values.sorted().reversed()

        val maxCount = counts.getOrNull(0) ?: 0
        val secondCount = counts.getOrNull(1) ?: 0

        // Check for straight
        // ACE(1), KING(2), QUEEN(3), JACK(4), TEN(5), NINE(6)
        val sortedFaces = value.map { it.face }.distinct().sortedBy { it.ordinal }
        val isStraight =
            sortedFaces.size == 5 &&
                sortedFaces.zipWithNext().all { (a, b) -> b.ordinal == a.ordinal + 1 }

        return when {
            maxCount == 5 -> HandValues.FIVE_OF_A_KIND

            maxCount == 4 -> HandValues.FOUR_OF_A_KIND

            // Full house (3 of one + 2 of another)
            maxCount == 3 && secondCount == 2 -> HandValues.FULL_HOUSE

            isStraight -> HandValues.STRAIGHT

            maxCount == 3 -> HandValues.THREE_OF_A_KIND

            maxCount == 2 && secondCount == 2 -> HandValues.TWO_PAIR

            maxCount == 2 -> HandValues.ONE_PAIR

            else -> HandValues.NO_VALUE
        }
    }

    fun calculateScore(): Int = evaluateHandValue().value
}
