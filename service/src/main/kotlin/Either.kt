package org.example

/**
 * Yuml class diagram:
[Either\<L,R\>|+Left(value: L);+Right(value: R)]
[Either\<L,R\>]^-.-[Left|+value: L]
[Either\<L,R\>]^-.-[Right|+value: R]
 */
sealed class Either<out L, out R> {
    data class Left<out L>(
        val value: L,
    ) : Either<L, Nothing>()

    data class Right<out R>(
        val value: R,
    ) : Either<Nothing, R>()
}

fun <R> success(value: R) = Either.Right(value)

fun <L> failure(error: L) = Either.Left(error)

typealias Success<S> = Either.Right<S>
typealias Failure<F> = Either.Left<F>

inline fun <L, R> Either<L, R>.onFailure(action: (L) -> Unit): Either<L, R> {
    if (this is Either.Left) {
        action(this.value)
    }
    return this
}

inline fun <L, R> Either<L, R>.onSuccess(action: (R) -> Unit): Either<L, R> {
    if (this is Either.Right) {
        action(this.value)
    }
    return this
}

inline fun <L, R> Either<L, R>.andThen(block: (R) -> Either<L, R>): Either<L, R> =
    when (this) {
        is Either.Left -> this
        is Either.Right -> block(this.value)
    }
