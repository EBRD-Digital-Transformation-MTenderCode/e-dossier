package com.procurement.dossier.application.model

import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.domain.util.extension.getDuplicate
import com.procurement.storage.domain.util.ValidationRule

fun <A : Any, T : Collection<A>?> noDuplicatesRule(
    attributeName: String,
    transform: (A) -> Any
): ValidationRule<T, DataErrors.Validation> =
    ValidationRule { received: T ->
        if (received == null) ValidationResult.ok()
        else {
            val transformed = mutableListOf<Any>()
            for (element in received)
                transformed.add(transform(element))
            val duplicate = transformed.getDuplicate { it }
            if (duplicate != null)
                ValidationResult.error(
                    DataErrors.Validation.UniquenessDataMismatch(
                        value = duplicate.toString(), name = attributeName
                    )
                )
            else
                ValidationResult.ok()
        }
    }

fun <T : Collection<Any>?> notEmptyRule(attributeName: String): ValidationRule<T, DataErrors.Validation> =
    ValidationRule { received: T ->
        if (received != null && received.isEmpty())
            ValidationResult.error(DataErrors.Validation.EmptyArray(attributeName))
        else
            ValidationResult.ok()
    }