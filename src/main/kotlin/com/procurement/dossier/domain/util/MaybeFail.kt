package com.procurement.dossier.domain.util

sealed class MaybeFail<out E> {

    companion object {
        fun <E> fail(value: E): MaybeFail<E> = Fail(value)
        fun <E> none(): MaybeFail<E> = None
    }

    abstract val error: E
    abstract val isFail: Boolean

    inline fun doOnFail(block: (error: E) -> Unit) {
        if (this.isFail) block(this.error)
    }

    fun <R> map(transform: (E) -> R): MaybeFail<R> = when (this) {
        is Fail -> Fail(transform(this.error))
        is None -> this
    }

    fun <R> recovery(function: (E) -> MaybeFail<R>): MaybeFail<R> = when (this) {
        is Fail -> function(this.error)
        is None -> this
    }

    inline fun also(function: (E) -> Unit): MaybeFail<E> {
        when (this) {
            is Fail -> function(error)
            is None -> Unit
        }
        return this
    }

    class Fail<out E>(override val error: E) : MaybeFail<E>() {
        override val isFail: Boolean = true
    }

    object None : MaybeFail<Nothing>() {
        override val error: Nothing
            get() = throw NoSuchElementException("MaybeFail do not contain value.")
        override val isFail: Boolean = false
    }
}
