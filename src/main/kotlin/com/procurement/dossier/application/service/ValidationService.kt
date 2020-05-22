package com.procurement.dossier.application.service

import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.application.service.params.ValidateRequirementResponseParams
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource
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
                return ValidationResult.error(
                    Fail.Incident.Transform.ParseFromDatabaseIncident(
                        jsonData = cnEntity.jsonData, exception = error.exception
                    )
                )
            }

        val requirementId = params.requirementResponse.requirement.id

        val requirements = createdCriteriaEntity.criteria
            ?.flatMap { it.requirementGroups }
            ?.flatMap { it.requirements }
            ?.takeIf { it.isNotEmpty() }
            ?: return ValidationResult.error(
                ValidationErrors.RequirementsNotFoundOnValidateRequirementResponse(params.cpid)
            )

        val requirement = requirements
            .find { requirement ->
                requirement.id == requirementId
            }
            ?: return ValidationResult.error(
                ValidationErrors.RequirementNotFoundOnValidateRequirementResponse(requirementId)
            )

        if (params.requirementResponse.value.dataType != requirement.dataType)
            return ValidationResult.error(
                ValidationErrors.RequirementDataTypeCompareError(
                    expectedDataType = requirement.dataType,
                    actualDataType = params.requirementResponse.value.dataType
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
}
