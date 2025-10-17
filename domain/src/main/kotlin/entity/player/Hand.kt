package org.example.entity.player

import org.example.entity.dice.Dice
import org.example.entity.dice.DiceFace

data class Hand(
    val value: List<Dice>,
) {
    fun evaluateHandValue(): HandValues {
        if (value.isEmpty()) return HandValues.NO_VALUE

        val faceCounts = value.groupingBy { it.face }.eachCount()
        val counts = faceCounts.values.sorted().reversed()

        // Check for straight (ACE, KING, QUEEN, JACK, TEN, NINE in sequence)
        val sortedFaces = value.map { it.face }.distinct().sortedBy { it.ordinal }
        val isStraight =
            sortedFaces.size == 5 &&
                sortedFaces == DiceFace.entries.sortedBy { it.ordinal }

        return when {
            counts[0] == 5 -> HandValues.FIVE_OF_A_KIND

            counts[0] == 4 -> HandValues.FOUR_OF_A_KIND

            // Full house (3 of one + 2 of another)
            counts[0] == 3 && counts[1] == 2 -> HandValues.FULL_HOUSE

            isStraight -> HandValues.STRAIGHT

            counts[0] == 3 -> HandValues.THREE_OF_A_KIND

            counts[0] == 2 && counts[1] == 2 -> HandValues.TWO_PAIR

            counts[0] == 2 -> HandValues.ONE_PAIR

            // No value
            else -> HandValues.NO_VALUE
        }
    }

    fun calculateScore(): Int = evaluateHandValue().value
}
