package org.example.entity.player

enum class Hand(
    val Five_of_a_Kind: Int = 7,
    val Four_of_a_Kind: Int = 6,
    val Full_House: Int = 5,
    val Straight: Int = 4,
    val Three_of_a_Kind: Int = 3,
    val Two_Pair: Int = 2,
    val One_Pair: Int = 1,
    val no_value: Int = 0,
)
