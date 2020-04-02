package com.procurement.dossier.application.service

import com.procurement.dossier.application.model.data.ExpectedValue
import com.procurement.dossier.application.model.data.MaxValue
import com.procurement.dossier.application.model.data.MinValue
import com.procurement.dossier.application.model.data.RangeValue
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.data.RequirementValue
import com.procurement.dossier.application.model.entity.CnEntity
import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.application.service.params.ValidateRequirementResponseParams
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.BadRequestErrors
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.infrastructure.model.entity.CreatedCriteriaEntity
import com.procurement.dossier.infrastructure.utils.tryToObject
import org.springframework.stereotype.Service

@Service
class ValidationService(private val criteriaRepository: CriteriaRepository, private val logger: Logger) {

    fun validateRequirementResponse(params: ValidateRequirementResponseParams): ValidationResult<Fail> {

        val createdCriteriaEntity = getCnEntityByCpid(cpid = params.cpid.toString())
            .doOnError { error -> return ValidationResult.error(error) }
            .get
            .jsonData
            .tryToObject(CreatedCriteriaEntity::class.java)
            .doOnError { error ->
                return ValidationResult.error(Fail.Incident.DatabaseIncident(exception = error.exception))
            }
            .get

        val requirementId = params.requirementResponse.requirement.id

        val requirements = createdCriteriaEntity.criteria
            ?.flatMap { it.requirementGroups }
            ?.flatMap { it.requirements }
            ?: return ValidationResult.error(ValidationErrors.RequirementNotFound(requirementId))

        if (requirements.isEmpty()) {
            return ValidationResult.error(ValidationErrors.RequirementNotFound(requirementId))
        }

        val requirement = requirements
            .find { requirement ->
                requirement.id == requirementId
            }
            ?: return ValidationResult.error(ValidationErrors.RequirementNotFound(requirementId))

        if (!isMatchingRequirementValues(requestValue = params.requirementResponse.value, dbValue = requirement.value))
            return ValidationResult.error(
                ValidationErrors.RequirementValueCompareError(
                    rvActual = params.requirementResponse.value,
                    rvExpected = requirement.value
                )
            )
        return ValidationResult.ok()
    }

    private fun isMatchingRequirementValues(requestValue: RequirementRsValue, dbValue: RequirementValue): Boolean =
        when (dbValue) {
            is ExpectedValue -> {
                when (dbValue) {
                    is ExpectedValue.AsNumber -> requestValue is RequirementRsValue.AsNumber
                    is ExpectedValue.AsString -> requestValue is RequirementRsValue.AsString
                    is ExpectedValue.AsInteger -> requestValue is RequirementRsValue.AsInteger
                    is ExpectedValue.AsBoolean -> requestValue is RequirementRsValue.AsBoolean
                }
            }
            is MaxValue -> {
                when (dbValue) {
                    is MaxValue.AsNumber -> requestValue is RequirementRsValue.AsNumber
                    is MaxValue.AsInteger -> requestValue is RequirementRsValue.AsInteger
                }
            }
            is MinValue -> {
                when (dbValue) {
                    is MinValue.AsNumber -> requestValue is RequirementRsValue.AsNumber
                    is MinValue.AsInteger -> requestValue is RequirementRsValue.AsInteger
                }
            }
            is RangeValue -> {
                when (dbValue) {
                    is RangeValue.AsNumber -> requestValue is RequirementRsValue.AsNumber
                    is RangeValue.AsInteger -> requestValue is RequirementRsValue.AsInteger
                }
            }
            else -> false
        }

    private fun getCnEntityByCpid(cpid: String): Result<CnEntity, Fail> {
        val cnEntity = criteriaRepository.tryFindBy(cpid = cpid)
            .doOnError { error -> return error.asFailure() }
            .get
            ?: return BadRequestErrors.EntityNotFound(entityName = "CnEntity", cpid = cpid)
                .asFailure()
        return cnEntity.asSuccess()
    }
}
