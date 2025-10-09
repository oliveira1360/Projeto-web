package org.example.entity

@JvmInline
value class Password(
    val value: String,
) {
    init {
        TODO("Check password type (String or Numeric Hash)")
        require(this.value.trim().isNotBlank())
        require(this.value.any { char -> char.isUpperCase() })
        require(this.value.any { char -> char.isDigit() })
    }
}

fun String.toPassword() = Password(this)
