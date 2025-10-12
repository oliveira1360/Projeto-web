package org.example.entity

@JvmInline
value class Name(
    val value: String,
) {
    init {
        require(this.value.trim().isNotBlank())
    }
}

fun String.toName() = Name(this)

fun String?.toNameOrNull() = this?.let { Name(it) }
