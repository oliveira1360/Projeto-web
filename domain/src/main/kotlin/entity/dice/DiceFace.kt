package org.example.entity.dice

enum class DiceFace(
    weight: Int,
) {
    ACE(1),
    KING(2),
    QUEEN(3),
    JACK(4),
    TEN(5),
    NINE(6),
}

fun Int.toDiceFace() = DiceFace.entries[this]
