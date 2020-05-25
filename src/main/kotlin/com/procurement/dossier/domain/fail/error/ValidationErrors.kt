package com.procurement.dossier.domain.fail.error

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.requirement.RequirementId
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource
import com.procurement.dossier.infrastructure.model.dto.ocds.RequirementDataType

sealed class ValidationErrors(
    numberError: String,
    override val description: String,
    val entityId: String? = null
) : Fail.Error(prefix = "VR-") {

    override val code: String = prefix + numberError

    class RequirementNotFoundOnValidateRequirementResponse(requirementId: RequirementId) : ValidationErrors(
        numberError = "10.5.1.1",
        description = "Requirement with id '$requirementId' not found.",
        entityId = requirementId
    )

    class RequirementsNotFoundOnValidateRequirementResponse(cpid: Cpid) : ValidationErrors(
        numberError = "10.5.1.3",
        description = "Requirements not found by cpid '$cpid'."
    )

    class RequirementDataTypeCompareError(actualDataType: RequirementDataType, expectedDataType: RequirementDataType) :
        ValidationErrors(
            numberError = "10.5.1.2",
            description = "Data type mismatch. Expected data type: '$expectedDataType', actual data type: '$actualDataType'."
        )

    class UnexpectedCriteriaSource(actual: CriteriaSource, expected: CriteriaSource) : ValidationErrors(
        numberError = "10.5.1.4",
        description = "Unexpected criteria.source value. Expected: '${expected}', actual: '${actual}'."
    )

    class InvalidPeriodDateComparedWithStartDate() : ValidationErrors(
        numberError = "5.6.3",
        description = "Period date must be after stored period start date."
    )

    class InvalidPeriodDateComparedWithEndDate() : ValidationErrors(
        numberError = "5.6.4",
        description = "Period date must precede stored period end date."
    )

    class SubmissionNotFound(id: String): ValidationErrors(
        numberError = "5.10.1",
        description = "Submission id(s) '$id' not found."
    )
}
