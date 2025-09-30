package org.example.entity

@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(isValidEmail(this.value))
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email.trim())
    }
}

fun String.toEmail() = Email(this)
