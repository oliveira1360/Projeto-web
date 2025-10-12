package org.example.entity

    data class URL(
        val value: String,
        ){
        init {
            require(value.isUrlValid())
        }

    }

fun String.isUrlValid() = if (this.trim().isNotBlank()) true else false

fun String.toURL() = URL(this)

fun String?.toUrlOrNull() = this?.let { URL(it) }
