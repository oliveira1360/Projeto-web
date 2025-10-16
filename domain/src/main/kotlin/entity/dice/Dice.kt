package org.example.entity.dice

data class Dice(
    val index: Int,
    val face: DiceFace,
) {
    override fun equals(other: Any?): Boolean = other is Dice && index == other.index && face == other.face

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + face.hashCode()
        return result
    }
}

fun Dice.roll(): Dice = Dice(this.index, DiceFace.entries.random())
