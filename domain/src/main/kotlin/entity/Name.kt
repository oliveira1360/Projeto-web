package org.example.entity

@JvmInline
value class Name(
    val value: String,
) {
    init {
        require(value.isNameValid())
    }
}

fun String.isNameValid() = if (this.trim().isNotBlank()) true else false

fun String.toName() = Name(this)

fun String?.toNameOrNull() = this?.let { Name(it) }
