package com.procurement.storage.domain.util

import com.procurement.dossier.domain.util.ValidationResult

inline fun <T, E> validationRule(crossinline block: (value: T) -> ValidationResult<E>) = ValidationRule.invoke(block)

interface ValidationRule<T, out E> {
    fun test(value: T): ValidationResult<E>

    companion object {
        inline operator fun <T, E> invoke(crossinline block: (value: T) -> ValidationResult<E>): ValidationRule<T, E> =
            object : ValidationRule<T, E> {
                override fun test(value: T) = block(value)
            }
    }
}
