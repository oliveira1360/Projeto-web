package org.example.entity.core

import java.security.MessageDigest

@JvmInline
value class PasswordHash private constructor(
    val value: String,
) {
    companion object {
        val sha256 = MessageDigest.getInstance("SHA-256")

        fun fromRaw(rawPassword: String): PasswordHash {
            require(rawPassword.isPasswordValid()) { "Invalid password format" }
            val hash = sha256.digest(rawPassword.toByteArray())
            val hex = hash.joinToString("") { "%02x".format(it) }
            return PasswordHash(hex)
        }

        fun fromRawOrNull(rawPassword: String?): PasswordHash? = rawPassword?.takeIf { it.isPasswordValid() }?.let { fromRaw(it) }

        fun fromHash(hash: String): PasswordHash {
            require(hash.length == 64 && hash.all { it.isDigit() || (it in 'a'..'f') }) { "Invalid hash format" }
            return PasswordHash(hash)
        }
    }
}

fun String.isPasswordValid(): Boolean {
    if (this.trim().isBlank()) return false
    if (this.none { char -> char.isUpperCase() }) return false
    if (this.none { char -> char.isDigit() }) return false
    return true
}

fun String.toPasswordFromRaw() = PasswordHash.fromRaw(this)

fun String?.toPasswordOrNullFromRaw() = PasswordHash.fromRawOrNull(this)

fun String.toPasswordFromHash() = PasswordHash.fromHash(this)
