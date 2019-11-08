package com.procurement.procurer.application.service

import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.model.data.ExpectedValue
import com.procurement.procurer.application.repository.CriteriaRepository
import com.procurement.procurer.application.service.context.GetCriteriaContext
import com.procurement.procurer.infrastructure.converter.createResponseData
import com.procurement.procurer.infrastructure.converter.toData
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.ResponseDto
import com.procurement.procurer.infrastructure.model.dto.cn.CreateCriteriaRequest
import com.procurement.procurer.application.model.data.GetCriteriaData
import com.procurement.procurer.application.model.data.RequestsForEvPanelsData
import com.procurement.procurer.application.model.data.Requirement
import com.procurement.procurer.application.model.data.RequirementValue
import com.procurement.procurer.application.service.command.checkActualItemRelation
import com.procurement.procurer.application.service.command.checkAnswerCompleteness
import com.procurement.procurer.application.service.command.checkAnsweredOnce
import com.procurement.procurer.application.service.command.checkArrays
import com.procurement.procurer.application.service.command.checkAwardCriteriaDetailsAreRequired
import com.procurement.procurer.application.service.command.checkAwardCriteriaDetailsEnum
import com.procurement.procurer.application.service.command.checkAwardCriteriaEnum
import com.procurement.procurer.application.service.command.checkCastCoefficient
import com.procurement.procurer.application.service.command.checkCoefficient
import com.procurement.procurer.application.service.command.checkCoefficientDataType
import com.procurement.procurer.application.service.command.checkCoefficientValueUniqueness
import com.procurement.procurer.application.service.command.checkConversionRelatesToEnum
import com.procurement.procurer.application.service.command.checkConversionRelation
import com.procurement.procurer.application.service.command.checkConversionWithoutCriteria
import com.procurement.procurer.application.service.command.checkCriteriaAndConversionAreRequired
import com.procurement.procurer.application.service.command.checkCriteriaWithAwardCriteria
import com.procurement.procurer.application.service.command.checkDataTypeValue
import com.procurement.procurer.application.service.command.checkDatatypeCompliance
import com.procurement.procurer.application.service.command.checkDateTime
import com.procurement.procurer.application.service.command.checkIdsUniqueness
import com.procurement.procurer.application.service.command.checkMinMaxValue
import com.procurement.procurer.application.service.command.checkPeriod
import com.procurement.procurer.application.service.command.checkRequirementRelationRelevance
import com.procurement.procurer.application.service.command.checkRequirements
import com.procurement.procurer.application.service.command.createCnEntity
import com.procurement.procurer.application.service.command.extractCreatedCriteria
import com.procurement.procurer.application.service.command.generateCreateCriteriaResponse
import com.procurement.procurer.application.service.command.processCriteria
import com.procurement.procurer.application.service.command.toEntity
import com.procurement.procurer.application.service.context.EvPanelsContext
import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaSource
import com.procurement.procurer.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.procurer.infrastructure.model.entity.CreatedCriteriaEntity
import com.procurement.procurer.infrastructure.utils.toObject

class CriteriaService(
    private val generationService: Generable,
    private val criteriaRepository: CriteriaRepository,
    private val medeiaValidationService: JsonValidationService
) {

    fun createCriteria(cm: CommandMessage): ResponseDto {
        val request: CreateCriteriaRequest = toObject(
            CreateCriteriaRequest::class.java,
            cm.data
        )
        val requestData = request.toData()
        val context = context(cm)

        val createdCriteria = processCriteria(
            requestData,
            generationService
        )
        val createdCriteriaEntity = createdCriteria.toEntity()
        val cn = createCnEntity(createdCriteriaEntity, context)

        criteriaRepository.save(cn)

        val cnResponse = toObject(CreatedCriteriaEntity::class.java, cn.jsonData)
        val responseData = generateCreateCriteriaResponse(
            cnResponse
        )

        return ResponseDto(id = cm.id, data = responseData)
    }

    fun checkCriteria(cm: CommandMessage): ResponseDto {
        val request = medeiaValidationService.validateCriteria(cm)

        request
            .toData()
            .checkConversionWithoutCriteria()
            .checkAwardCriteriaDetailsAreRequired()  // FReq-1.1.1.22
            .checkCriteriaAndConversionAreRequired() // FReq-1.1.1.23
            .checkCoefficientValueUniqueness()       // FReq-1.1.1.24
            .checkCriteriaWithAwardCriteria()        // FReq-1.1.1.27

            .checkActualItemRelation()   // FReq-1.1.1.3
            .checkDatatypeCompliance()   // FReq-1.1.1.4
            .checkMinMaxValue()          // FReq-1.1.1.5
            .checkDateTime()             // FReq-1.1.1.6
            .checkRequirements()         // FReq-1.1.1.8

            .checkConversionRelation()         // FReq-1.1.1.9  & FReq-1.1.1.10
            .checkCoefficient()                // FReq-1.1.1.11
            .checkCoefficientDataType()        // FReq-1.1.1.12
            .checkCastCoefficient()            // FReq-1.1.1.13
            .checkConversionRelatesToEnum()    // FReq-1.1.1.14
            .checkAwardCriteriaEnum()          // FReq-1.1.1.15
            .checkAwardCriteriaDetailsEnum()   // FReq-1.1.1.15
            .checkArrays()                     // FReq-1.1.1.16

        return ResponseDto(data = "ok")
    }

    fun checkResponses(cm: CommandMessage): ResponseDto {
        val request = medeiaValidationService.validateResponses(cm)
        val context = context(cm)
        val cnEntity = criteriaRepository.findBy(context.cpid) ?: throw ErrorException(
            error = ErrorType.ENTITY_NOT_FOUND,
            message = "Cannot found record with cpid=${context.cpid}."
        )
        val createdCriteria = cnEntity
            .extractCreatedCriteria()
            .toData()

        if (createdCriteria.criteria != null && request.bid.requirementResponses == null) throw ErrorException(
            error = ErrorType.INVALID_REQUIREMENT_RESPONSE,
            message = "No requirement responses found for requirement from criteria"
        )

        request
            .toData()
            .checkRequirementRelationRelevance(createdCriteria) // FReq-1.2.1.1
            .checkAnswerCompleteness(createdCriteria)           // FReq-1.2.1.2
            .checkAnsweredOnce()                                // FReq-1.2.1.3
            .checkDataTypeValue(createdCriteria)                // FReq-1.2.1.4
            .checkPeriod()                                      // FReq-1.2.1.5 & FReq-1.2.1.7
            .checkIdsUniqueness()                               // FReq-1.2.1.6

        return ResponseDto(data = "ok")
    }

    fun getCriteriaDetails(context: GetCriteriaContext): GetCriteriaData? {
        return criteriaRepository.findBy(context.cpid)?.let { cnEntity ->
            cnEntity.extractCreatedCriteria()
                .toData()
                .let { createResponseData(it) }
        }
    }

    fun createRequestsForEvPanels(context: EvPanelsContext): RequestsForEvPanelsData {
        return RequestsForEvPanelsData(
            criteria = RequestsForEvPanelsData.Criteria(
                id = generationService.generatePermanentCriteriaId(),
                title = "",
                description = "",
                source = CriteriaSource.PROCURING_ENTITY,
                requirementGroups = listOf(
                    RequestsForEvPanelsData.Criteria.RequirementGroup(
                        id = generationService.generatePermanentRequirementGroupId(),
                        requirements = listOf(
                            Requirement(
                                id = generationService.generatePermanentRequirementId().toString(),
                                title = "",
                                dataType = RequirementDataType.BOOLEAN,
                                value = ExpectedValue.of(false),
                                period = null,
                                description = null
                            )
                        )
                    )
                )

            )
        )
    }

    private fun context(cm: CommandMessage): ContextRequest {
        fun missingArgumentException(argument: String): Nothing =
            throw ErrorException(
                error = ErrorType.CONTEXT,
                message = "Missing the '${argument}' attribute in context."
            )

        val cpid = cm.context.cpid ?: missingArgumentException("cpid")
        val owner = cm.context.owner ?: missingArgumentException("owner")

        return ContextRequest(
            cpid = cpid,
            owner = owner
        )
    }

    data class ContextRequest(
        val cpid: String,
        val owner: String
    )
}

