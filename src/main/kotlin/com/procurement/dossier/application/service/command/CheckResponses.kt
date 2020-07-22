package com.procurement.dossier.application.service.command

import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.data.CheckResponsesData
import com.procurement.dossier.application.model.data.CreatedCriteria
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.entity.CnEntity
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.dossier.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.dossier.infrastructure.model.entity.CreatedCriteriaEntity
import com.procurement.dossier.infrastructure.utils.toObject
import java.time.Clock
import java.time.LocalDateTime

fun CnEntity.extractCreatedCriteria(): CreatedCriteriaEntity {
    return toObject(CreatedCriteriaEntity::class.java, this.jsonData)
}

fun CheckResponsesData.checkRequirementRelationRelevance(createdCriteria: CreatedCriteria): CheckResponsesData {
    if (this.bid.requirementResponses.isEmpty() && createdCriteria.criteria == null) return this

    val requirementResponses = this.bid.requirementResponses

    val criteriaDb = createdCriteria.criteria ?: throw ErrorException(
        error = ErrorType.ENTITY_NOT_FOUND,
        message = "Bid.RequirementResponse object is present in request but no record found in DB."
    )
    val requirementsDb = criteriaDb.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .toList()

    requirementResponses
        .map { it.requirement }
        .forEach { requirementRequest ->
            requirementsDb.find { it.id == requirementRequest.id } ?: throw ErrorException(
                error = ErrorType.INVALID_REQUIREMENT_VALUE,
                message = "No requirement founded  by passed id=${requirementRequest.id}. "
            )
        }

    return this
}

fun CheckResponsesData.checkAnswerCompleteness(createdCriteria: CreatedCriteria): CheckResponsesData {

    if (this.bid.requirementResponses.isEmpty() && createdCriteria.criteria == null) return this
    val requirementResponses = this.bid.requirementResponses

    val criteriaDb = createdCriteria.criteria ?: throw ErrorException(
        error = ErrorType.ENTITY_NOT_FOUND,
        message = "Bid.RequirementResponse object is present in request but no record found in DB."
    )

    val lotRequirements = criteriaDb.asSequence()
        .filter { it.relatedItem == this.bid.relatedLots[0] }
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .map { it.id }
        .toList()

    val itemsRequirements = this.items.asSequence()
        .map { item ->
            criteriaDb.find { it.relatedItem == item.id }
        }
        .filterNotNull()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .map { it.id }
        .toList()

    val requirementRequest = requirementResponses.asSequence()
        .map { it.requirement }
        .map { it.id }
        .toList()

    val totalRequirements = lotRequirements + itemsRequirements
    val answered = (totalRequirements).intersect(requirementRequest)
    if (answered.size != totalRequirements.size) throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "For lots and items founded ${totalRequirements.size} requirement in DB but answered for ${answered.size}. " +
            "Ignored requirements: ${totalRequirements.minus(answered)} "
    )

    val tenderRequirements = criteriaDb.asSequence()
        .filter { it.relatesTo == CriteriaRelatesTo.TENDERER || it.relatesTo == null }
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .map { it.id }
        .toList()

    val answeredTender = (tenderRequirements).intersect(requirementRequest)
    if (answeredTender.size != tenderRequirements.size) throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "For tenderer and tender founded ${tenderRequirements.size} requirement in DB but answered for ${answeredTender.size}. " +
            "Ignored requirements: ${tenderRequirements.minus(answeredTender)} "
    )

    if (requirementRequest.size != totalRequirements.size + tenderRequirements.size) throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "Need to answer on ${totalRequirements.size + tenderRequirements.size} requirements, but answered on ${requirementRequest.size}. "
    )

    return this
}

fun CheckResponsesData.checkAnsweredOnce(): CheckResponsesData {

    if (this.bid.requirementResponses.isEmpty()) return this
    val requirementResponses = this.bid.requirementResponses

    val requirementIds = requirementResponses.asSequence()
        .map { it.requirement }
        .map { it.id }
        .toList()

    val uniqueAnswer = requirementIds.toSet()

    if (uniqueAnswer.size != requirementIds.size) throw throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "Founded duplicated answer. Duplicated: ${requirementIds.groupBy { it }.filter { it.value.size > 1 }.keys}"
    )

    return this
}

fun CheckResponsesData.checkDataTypeValue(createdCriteria: CreatedCriteria): CheckResponsesData {
    fun dataTypeMismatchException(
        requirementResponseDatatype: RequirementRsValue,
        requirementDbDataType: RequirementDataType,
        requirementResponse: CheckResponsesData.Bid.RequirementResponse
    ): Nothing = throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "RequirementResponse dataType= ${requirementResponseDatatype.javaClass}, " +
            "Requirement dataType (DB) = ${requirementDbDataType}. " +
            "ReqirementResponse.id=${requirementResponse.id}." +
            "ReqirementResponse.requirement.id=${requirementResponse.requirement.id}. "
    )

    if (this.bid.requirementResponses.isEmpty() && createdCriteria.criteria == null) return this
    val requirementResponses = this.bid.requirementResponses

    val criteriaDb = createdCriteria.criteria ?: throw ErrorException(
        error = ErrorType.ENTITY_NOT_FOUND,
        message = "Bid.RequirementResponse object is present in request but no record found in DB. "
    )

    val requirementDb = criteriaDb.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .toList()

    requirementResponses.forEach { requirementResponse ->
        val requirement = requirementDb.find { requirementResponse.requirement.id == it.id } ?: throw ErrorException(
            error = ErrorType.INVALID_REQUIREMENT_VALUE,
            message = "Cannot find requirement id DB by id=${requirementResponse.requirement.id}. "
        )

        when (requirementResponse.value) {
            is RequirementRsValue.AsString -> if (requirement.dataType != RequirementDataType.STRING)
                dataTypeMismatchException(
                    requirementResponseDatatype = requirementResponse.value,
                    requirementDbDataType = requirement.dataType,
                    requirementResponse = requirementResponse
                )
            is RequirementRsValue.AsBoolean -> if (requirement.dataType != RequirementDataType.BOOLEAN)
                dataTypeMismatchException(
                    requirementResponseDatatype = requirementResponse.value,
                    requirementDbDataType = requirement.dataType,
                    requirementResponse = requirementResponse
                )
            is RequirementRsValue.AsInteger -> if (requirement.dataType != RequirementDataType.INTEGER)
                dataTypeMismatchException(
                    requirementResponseDatatype = requirementResponse.value,
                    requirementDbDataType = requirement.dataType,
                    requirementResponse = requirementResponse
                )
            is RequirementRsValue.AsNumber -> if (requirement.dataType != RequirementDataType.NUMBER)
                dataTypeMismatchException(
                    requirementResponseDatatype = requirementResponse.value,
                    requirementDbDataType = requirement.dataType,
                    requirementResponse = requirementResponse
                )
        }
    }

    return this
}

fun CheckResponsesData.checkPeriod(): CheckResponsesData {
    fun invalidPeriodExpection(endDate: LocalDateTime, currentTime: LocalDateTime): Nothing = throw ErrorException(
        error = ErrorType.INVALID_PERIOD_VALUE,
        message = "Period.endDate specified in RequirementResponse cannot be greater or equals to current time. " +
            "EndDate = ${JsonDateTimeSerializer.serialize(endDate)}, Current time = ${JsonDateTimeSerializer.serialize(
                currentTime
            )}"
    )

    fun invalidPeriodExpection(period: CheckResponsesData.Bid.RequirementResponse.Period): Nothing = throw ErrorException(
        error = ErrorType.INVALID_PERIOD_VALUE,
        message = "Period.startDate specified in RequirementResponse cannot be greater or equals to period.endDate. " +
            "StratDate = ${JsonDateTimeSerializer.serialize(period.startDate)}, " +
            "EndDate = ${JsonDateTimeSerializer.serialize(period.endDate)}"
    )

    if (this.bid.requirementResponses.isEmpty()) return this
    val requirementResponses = this.bid.requirementResponses

    requirementResponses.map { it.period }
        .filterNotNull()
        .forEach {
            val currentTime = LocalDateTime.now(Clock.systemUTC())
            if (it.endDate >= currentTime) invalidPeriodExpection(endDate = it.endDate, currentTime = currentTime)
            if (it.startDate >= it.endDate) invalidPeriodExpection(period = it)
        }

    return this
}

fun CheckResponsesData.checkIdsUniqueness(): CheckResponsesData {
    if (this.bid.requirementResponses.isEmpty()) return this
    val requirementResponses = this.bid.requirementResponses

    val requirementResponseIds = requirementResponses.map { it.id }
    val requirementResponseUniqueIds = requirementResponseIds.toSet()
    if (requirementResponseIds.size != requirementResponseUniqueIds.size) throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "Id in RequirementResponse object must be unique. All ids: ${requirementResponseIds}. " +
            "Duplicated ids: ${requirementResponseIds.groupBy { it }.filter { it.value.size > 1 }.keys}"
    )

    return this
}

