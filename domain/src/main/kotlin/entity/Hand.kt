package org.example.entity

data class Hand(
    val dices: List<Dice>,
    val fixed: List<Dice>,
)

fun Hand.fixDice(idx: Int): Hand = Hand(this.dices, this.fixed + this.dices[idx])

fun Hand.unfixDice(idx: Int): Hand = Hand(this.dices, this.fixed - this.fixed[idx])

fun Hand.roll(): Hand {
    val newDices =
        this.dices.map {
            if (it !in this.fixed) {
                it.roll()
            } else {
                it
            }
        }
    return Hand(newDices, this.fixed)
}
