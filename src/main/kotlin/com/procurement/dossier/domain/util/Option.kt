package com.procurement.dossier.domain.util

sealed class Option<out T> {

    companion object {
        fun <T> pure(value: T): Option<T> = Some(value)
        fun <T> none(): Option<T> = None
        fun <T> fromNullable(value: T?): Option<T> = if (value != null) Some(value) else None
    }

    abstract val get: T
    abstract val orNull: T?

    fun <E : Exception> orThrow(block: () -> E): T = when (this) {
        is Some -> get
        is None -> throw block()
    }

    abstract val isEmpty: Boolean
    abstract val nonEmpty: Boolean
    val isDefined: Boolean
        get() = !isEmpty

    fun <R> map(transform: (T) -> R): Option<R> = when (this) {
        is Some -> Some(transform(this.get))
        is None -> this
    }

    fun <R> bind(function: (T) -> Option<R>): Option<R> = when (this) {
        is Some -> function(this.get)
        is None -> this
    }
}

class Some<out T>(value: T) : Option<T>() {
    override val get: T = value
    override val orNull: T = value
    override val isEmpty: Boolean = false
    override val nonEmpty: Boolean = !isEmpty
}

object None : Option<Nothing>() {
    override val get: Nothing
        get() = throw NoSuchElementException("Option do not contain value.")
    override val orNull: Nothing? = null
    override val isEmpty: Boolean = true
    override val nonEmpty: Boolean = !isEmpty
}
