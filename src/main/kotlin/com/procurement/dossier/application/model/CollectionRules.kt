package com.procurement.dossier.application.model

import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.domain.util.extension.getDuplicate
import com.procurement.storage.domain.util.ValidationRule

fun <T : Collection<Any>?> noDuplicatesRule(attributeName: String): ValidationRule<T, DataErrors.Validation> =
    ValidationRule { received: T ->
        val duplicate = received?.getDuplicate { it }
        if (duplicate != null)
            ValidationResult.error(
                DataErrors.Validation.UniquenessDataMismatch(
                    value = duplicate.toString(), name = attributeName
                )
            )
        else
            ValidationResult.ok()
    }

fun <T : Collection<Any>?> notEmptyRule(attributeName: String): ValidationRule<T, DataErrors.Validation> =
    ValidationRule { received: T ->
        if (received != null && received.isEmpty())
            ValidationResult.error(DataErrors.Validation.EmptyArray(attributeName))
        else
            ValidationResult.ok()
    }