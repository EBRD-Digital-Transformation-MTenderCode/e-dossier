package com.procurement.dossier.domain.fail.error

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.data.RequirementValue
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource

sealed class ValidationErrors(
    numberError: String,
    override val description: String,
    val entityId: String? = null
) : Fail.Error(prefix = "VR-") {

    override val code: String = prefix + numberError

    class RequirementNotFound(requirementId: String) : ValidationErrors(
        numberError = "10.5.1.1",
        description = "Requirement with id '$requirementId' not found.",
        entityId = requirementId
    )

    class RequirementValueCompareError(rvActual: RequirementRsValue, rvExpected: RequirementValue) : ValidationErrors(
        numberError = "10.5.1.2",
        description = "Requirement.dataType mismatch. ${rvActual} != ${rvExpected}"
    )

    class UnexpectedCriteriaSource(actual: CriteriaSource, expected: CriteriaSource) : ValidationErrors(
        numberError = "10.5.1.4",
        description = "Unexpected criteria.source value. Expected: '${expected}', actual: '${actual}'."
    )
}
