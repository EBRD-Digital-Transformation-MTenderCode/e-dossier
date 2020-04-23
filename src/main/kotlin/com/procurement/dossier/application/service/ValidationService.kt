package com.procurement.dossier.application.service

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.application.service.params.ValidateRequirementResponseParams
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource
import com.procurement.dossier.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.dossier.infrastructure.model.entity.CreatedCriteriaEntity
import com.procurement.dossier.infrastructure.utils.tryToObject
import org.springframework.stereotype.Service

@Service
class ValidationService(private val criteriaRepository: CriteriaRepository) {

    fun validateRequirementResponse(params: ValidateRequirementResponseParams): ValidationResult<Fail> {

        val cnEntity = criteriaRepository.tryFindBy(cpid = params.cpid)
            .doReturn { error -> return ValidationResult.error(error) }
            ?: return ValidationResult.error(
                ValidationErrors.RequirementsNotFoundOnValidateRequirementResponse(cpid = params.cpid)
            )

        val createdCriteriaEntity = cnEntity.jsonData
            .tryToObject(CreatedCriteriaEntity::class.java)
            .doReturn { error ->
                return ValidationResult.error(Fail.Incident.DatabaseIncident(exception = error.exception))
            }

        val requirementId = params.requirementResponse.requirement.id

        val requirements = createdCriteriaEntity.criteria
            ?.flatMap { it.requirementGroups }
            ?.flatMap { it.requirements }
            ?: return ValidationResult.error(
                ValidationErrors.RequirementsNotFoundOnValidateRequirementResponse(params.cpid)
            )

        if (requirements.isEmpty()) {
            return ValidationResult.error(
                ValidationErrors.RequirementsNotFoundOnValidateRequirementResponse(params.cpid)
            )
        }

        val requirement = requirements
            .find { requirement ->
                requirement.id == requirementId
            }
            ?: return ValidationResult.error(
                ValidationErrors.RequirementNotFoundOnValidateRequirementResponse(requirementId)
            )

        if (!isMatchingRequirementValues(value = params.requirementResponse.value, dataType = requirement.dataType))
            return ValidationResult.error(
                ValidationErrors.RequirementValueCompareError(
                    rvActual = params.requirementResponse.value,
                    rvExpected = requirement.value
                )
            )

        val criteria = createdCriteriaEntity.criteria
            .find { criteria ->
                criteria.requirementGroups
                    .flatMap { it.requirements }
                    .any { it.id == requirementId }
            }
            ?: return ValidationResult.error(
                ValidationErrors.RequirementNotFoundOnValidateRequirementResponse(
                    requirementId
                )
            )

        val expectedSource = CriteriaSource.PROCURING_ENTITY
        if (criteria.source != null && criteria.source != expectedSource)
            return ValidationResult.error(
                ValidationErrors.UnexpectedCriteriaSource(actual = criteria.source, expected = expectedSource)
            )

        return ValidationResult.ok()
    }

    private fun isMatchingRequirementValues(value: RequirementRsValue, dataType: RequirementDataType): Boolean =
        when (dataType) {
            RequirementDataType.NUMBER -> value is RequirementRsValue.AsNumber
            RequirementDataType.BOOLEAN -> value is RequirementRsValue.AsBoolean
            RequirementDataType.STRING -> value is RequirementRsValue.AsString
            RequirementDataType.INTEGER -> value is RequirementRsValue.AsInteger
        }
}
