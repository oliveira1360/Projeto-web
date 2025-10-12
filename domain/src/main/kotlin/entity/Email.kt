package org.example.entity

@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(value.isValidEmail())
    }


}

private fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return emailRegex.matches(this.trim())
}
fun String.toEmail() = Email(this)

fun String?.toEmailOrNull() = this?.let { Email(it) }
