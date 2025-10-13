package org.example.entity

@JvmInline
value class Password(
    val value: String,
) {
    init {
        require(value.isPasswordValid())
    }
}

fun String.isPasswordValid(): Boolean {
    if (this.trim().isBlank()) return false
    if (this.none { char -> char.isUpperCase() }) return false
    if (this.none { char -> char.isDigit() }) return false
    return true
}

fun String.toPassword() = Password(this)

fun String?.toPasswordOrNull() = this?.let { Password(it) }
