package org.example.entity

data class URL(
    val value: String,
    )

fun String.toURL() = URL(this)

fun String?.toUrlOrNull() = this?.let { URL(it) }
