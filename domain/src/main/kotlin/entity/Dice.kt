package org.example.entity

data class Dice(
    val index: Int,
    val face: DiceFace
) {
    override fun equals(other: Any?): Boolean {
        return other is Dice && index == other.index && face == other.face
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + face.hashCode()
        return result
    }
}

fun Dice.roll(): Dice {
    return Dice(this.index, DiceFace.entries.random())
}