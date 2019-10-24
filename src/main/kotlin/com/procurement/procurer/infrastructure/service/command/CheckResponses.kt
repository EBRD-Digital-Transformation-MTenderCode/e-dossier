package com.procurement.procurer.infrastructure.service.command

import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.model.data.CreatedCriteria
import com.procurement.procurer.application.model.data.RequirementRsValue
import com.procurement.procurer.application.model.entity.CnEntity
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.procurer.infrastructure.model.dto.cn.CheckResponsesData
import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.procurer.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.procurer.infrastructure.model.entity.CreatedCriteriaEntity
import com.procurement.procurer.infrastructure.utils.toJson
import com.procurement.procurer.infrastructure.utils.toObject
import java.time.Clock
import java.time.LocalDateTime

fun CnEntity.extractCreatedCriteria(): CreatedCriteriaEntity {
    return toObject(CreatedCriteriaEntity::class.java, this.jsonData)
}

fun CheckResponsesData.checkRequirementRelationRelevance(createdCriteria: CreatedCriteria): CheckResponsesData {

    val requirementResponses = this.bid.requirementResponses ?: return this
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
                message = "No requirement founded  by passed id=${requirementRequest.id}. Ids from DB: ${requirementsDb.map { it.id }}"
            )
        }

    return this
}

fun CheckResponsesData.checkAnswerCompleteness(createdCriteria: CreatedCriteria): CheckResponsesData {

    val requirementResponses = this.bid.requirementResponses ?: return this
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
    if (answered.size < totalRequirements.size) throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "For lots and items founded ${totalRequirements.size} requirement in DB but answered only for ${answered.size}. " +
            "Ignored requirements: ${totalRequirements.minus(answered)} "
    )

    val tenderRequirements = criteriaDb.asSequence()
        .filter { it.relatesTo == CriteriaRelatesTo.TENDERER || it.relatesTo == null }
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .map { it.id }
        .toList()

    val answeredTender = (tenderRequirements).intersect(requirementRequest)
    if (answeredTender.size < tenderRequirements.size) throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "For tenderer and tender founded ${tenderRequirements.size} requirement in DB but answered only for ${answeredTender.size}. " +
            "Ignored requirements: ${tenderRequirements.minus(answeredTender)} "
    )

    return this
}

fun CheckResponsesData.checkAnsweredOnce(): CheckResponsesData {
    val requirementResponses = this.bid.requirementResponses ?: return this

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
        message = "RequirementResponse dataType= ${requirementResponseDatatype.javaClass}, \n" +
            "Requirement dataType (DB) = ${requirementDbDataType.value()}. \n" +
            "ReqirementResponse.id=${requirementResponse.id}.\n" +
            "ReqirementResponse.requirement.id=${requirementResponse.requirement.id}\n"
    )

    val requirementResponses = this.bid.requirementResponses ?: return this

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
        message = "Period.endDate specified in RequirementResponse cannot be greater than current time. " +
            "EndDate = ${JsonDateTimeSerializer.serialize(endDate)}, Current time = ${JsonDateTimeSerializer.serialize(
                currentTime
            )}"
    )

    fun invalidPeriodExpection(period: CheckResponsesData.Bid.RequirementResponse.Period): Nothing = throw ErrorException(
        error = ErrorType.INVALID_PERIOD_VALUE,
        message = "Period.startDate specified in RequirementResponse cannot be greater than period.endDate. " +
            "StratDate = ${JsonDateTimeSerializer.serialize(period.startDate)}, " +
            "EndDate = ${JsonDateTimeSerializer.serialize(period.endDate)}"
    )

    val requirementResponses = this.bid.requirementResponses ?: return this

    requirementResponses.map { it.period }
        .filterNotNull()
        .forEach {
            val currentTime = LocalDateTime.now(Clock.systemUTC())
            if (it.endDate >= currentTime) invalidPeriodExpection(endDate = it.endDate, currentTime = currentTime)
            if (it.startDate > it.endDate) invalidPeriodExpection(period = it)
        }

    return this
}

fun CheckResponsesData.checkIdsUniqueness(): CheckResponsesData {
    val requirementResponses = this.bid.requirementResponses ?: return this

    val requirementResponseIds = requirementResponses.map { it.id }
    val requirementResponseUniqueIds = requirementResponseIds.toSet()
    if (requirementResponseIds.size != requirementResponseUniqueIds.size) throw ErrorException(
        error = ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "Id in RequirementResponse object must be unique. All ids: ${requirementResponseIds}. " +
            "Duplicated ids: ${requirementResponseIds.groupBy { it }.filter { it.value.size > 1 }.keys}"
    )

    return this
}
