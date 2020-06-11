package com.procurement.dossier.domain.util

sealed class ValidationResult<out E> {

    companion object {
        fun <E> ok(): ValidationResult<E> = Ok
        fun <E> error(value: E): ValidationResult<E> = Fail(value)
    }

    abstract val error: E
    abstract val isOk: Boolean
    abstract val isError: Boolean

    inline fun doOnError(block: (E) -> Unit) {
        if (this.isError) block(this.error)
    }

    val asOption: Option<E>
        get() = when (this) {
            is Fail -> Option.pure(error)
            is Ok -> Option.none()
        }

    fun <R> map(transform: (E) -> R): ValidationResult<R> = when (this) {
        is Ok -> this
        is Fail -> Fail(transform(this.error))
    }

    fun <R> bind(function: (E) -> ValidationResult<R>): ValidationResult<R> = when (this) {
        is Ok -> this
        is Fail -> function(this.error)
    }

    object Ok : ValidationResult<Nothing>() {
        override val error: Nothing
            get() = throw NoSuchElementException("Validation result does not contain error.")
        override val isOk: Boolean = true
        override val isError: Boolean = !isOk
    }

    class Fail<out E>(value: E) : ValidationResult<E>() {
        override val error: E = value
        override val isOk: Boolean = false
        override val isError: Boolean = !isOk
    }
}
