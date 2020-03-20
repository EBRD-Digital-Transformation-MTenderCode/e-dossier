package com.procurement.dossier.domain.fail.error

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.data.RequirementValue
import com.procurement.dossier.domain.fail.Fail

sealed class ValidationErrors(
    numberError: String,
    override val description: String
) : Fail.Error(prefix = "VE-") {

    override val code: String = prefix + numberError

    class RequirementNotFound(requirementId: String) : ValidationErrors(
        numberError = "01", description = "Requirement with id '$requirementId' not found."
    )

    class RequirementValueCompareError(rvActual: RequirementRsValue, rvExpected: RequirementValue) : ValidationErrors(
        numberError = "02",
        description = "Requirement.dataType mismatch. ${rvActual} != ${rvExpected}"
    )
}
