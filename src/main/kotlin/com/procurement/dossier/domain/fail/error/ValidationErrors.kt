package com.procurement.dossier.domain.fail.error

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.requirement.RequirementId
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.extension.format
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource
import com.procurement.dossier.infrastructure.model.dto.ocds.Operation
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import com.procurement.dossier.infrastructure.model.dto.ocds.RequirementDataType
import java.time.LocalDateTime

sealed class ValidationErrors(
    numberError: String,
    override val description: String,
    val entityId: String? = null,
    prefix: String = "VR.COM-"
) : Fail.Error(prefix = prefix) {

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

    class InvalidPeriodDateComparedWithStartDate(requestDate: LocalDateTime, startDate: LocalDateTime) :
        ValidationErrors(
            numberError = "5.6.3",
            description = "Period date '${requestDate.format()}' must be after stored period start date '${startDate.format()}'."
        )

    class InvalidPeriodDateComparedWithEndDate(requestDate: LocalDateTime, endDate: LocalDateTime) : ValidationErrors(
        numberError = "5.6.4",
        description = "Period date '${requestDate.format()}' must precede stored period end date '${endDate.format()}'."
    )

    sealed class SubmissionNotFoundFor : ValidationErrors {

        constructor(numberError: String, id: SubmissionId) :
            super(numberError = numberError, description = "Submission id(s) '$id' not found.")

        constructor(numberError: String, ids: List<SubmissionId>) :
            super(numberError = numberError, description = "Submission id(s) '${ids.joinToString()}' not found.")

        class GetSubmissionStateByIds(id: SubmissionId) : SubmissionNotFoundFor(id = id, numberError = "5.10.1")
        class SetStateForSubmission(id: SubmissionId) : SubmissionNotFoundFor(id = id, numberError = "5.11.1")
        class CheckAccessToSubmission(id: SubmissionId) : SubmissionNotFoundFor(id = id, numberError = "5.9.3")
        class SubmissionsByQualificationIds(ids: List<SubmissionId>) : SubmissionNotFoundFor(
            ids = ids, numberError = "5.17.1"
        )
    }

    class PeriodNotFound(cpid: Cpid, ocid: Ocid) : ValidationErrors(
        numberError = "5.6.1",
        description = "Record of a period by cpid: '$cpid' and ocid: '$ocid' not found."
    )

    class InvalidToken() : ValidationErrors(
        numberError = "5.9.1",
        description = "Received token does not match submission token."
    )

    class InvalidOwner() : ValidationErrors(
        numberError = "5.9.2",
        description = "Received owner does not match submission owner."
    )

    sealed class RecordNotFoundFor(cpid: Cpid, ocid: Ocid, numberError: String) : ValidationErrors(
        numberError = numberError,
        description = "No record found by cpid '$cpid' and ocid '$ocid'."
    ) {
        class GetOrganizations(cpid: Cpid, ocid: Ocid) : RecordNotFoundFor(cpid, ocid, numberError = "5.12.1")
    }

    class OrganizationsNotFound(cpid: Cpid, ocid: Ocid) : ValidationErrors(
        numberError = "5.12.2",
        description = "No organization found by cpid '$cpid' and ocid '$ocid'."
    )

    sealed class PeriodEndDateNotFoundFor(cpid: Cpid, ocid: Ocid, numberError: String) : ValidationErrors(
        numberError = numberError,
        description = "No period end date found by cpid '$cpid' and ocid '$ocid'."
    ) {
        class VerifySubmissionPeriodEnd(cpid: Cpid, ocid: Ocid) :
            PeriodEndDateNotFoundFor(cpid, ocid, numberError = "5.15.1")

        class GetSubmissionPeriodEndDate(cpid: Cpid, ocid: Ocid) :
            PeriodEndDateNotFoundFor(cpid, ocid, numberError = "5.14.1")
    }

    sealed class Duplicate(value: String, entityName: String, numberError: String) : ValidationErrors(
        numberError = numberError,
        description = "Value '$value' is not unique in '$entityName'."
    ) {
        class Candidate(id: String) : Duplicate(
            value = id,
            entityName = "candidates.id",
            numberError = "5.7.1"
        )

        class OrganizationDocument(id: DocumentId) : Duplicate(
            value = id,
            entityName = "documents.id",
            numberError = "5.7.2"
        )

        class PersonBusinessFunction(id: String) : Duplicate(
            value = id,
            entityName = "candidates.persons.businessFunctions.id",
            numberError = "5.7.3"
        )

        class PersonDocument(id: DocumentId) : Duplicate(
            value = id,
            entityName = "candidates.persons.businessFunctions.documents.id",
            numberError = "5.7.4"
        )
    }

    sealed class EntityNotFound(description: String) : ValidationErrors(numberError = "17", description = description, prefix = "VR-") {

        class SubmissionValidStateRule(
            country: String,
            pmd: ProcurementMethod,
            operationType: Operation
        ) : EntityNotFound("Rule for submission state not found by country '$country', pmd '${pmd.name}', operationType '$operationType'.")

        class SubmissionMinimumQuantityRule(
            country: String,
            pmd: ProcurementMethod,
            operationType: Operation
        ) : EntityNotFound("Rule for submission minimum quantity not found by country '$country', pmd '${pmd.name}', operationType '$operationType'.")
    }
}
