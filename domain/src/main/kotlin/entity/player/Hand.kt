package org.example.entity.player

enum class Hand(
    val value: Int,
) {
    FIVE_OF_A_KIND(7),
    FOUR_OF_A_KIND(6),
    FULL_HOUSE(5),
    STRAIGHT(4),
    THREE_OF_A_KIND(3),
    TWO_PAIR(2),
    ONE_PAIR(1),
    NO_VALUE(0),
}
