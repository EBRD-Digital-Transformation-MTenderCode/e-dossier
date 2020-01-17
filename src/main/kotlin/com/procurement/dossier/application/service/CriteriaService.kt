package com.procurement.dossier.application.service

import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.data.CreatedCriteria
import com.procurement.dossier.application.model.data.GetCriteriaData
import com.procurement.dossier.application.model.data.NoneValue
import com.procurement.dossier.application.model.data.RequestsForEvPanelsData
import com.procurement.dossier.application.model.data.Requirement
import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.application.service.command.buildCriteria
import com.procurement.dossier.application.service.command.checkActualItemRelation
import com.procurement.dossier.application.service.command.checkAnswerCompleteness
import com.procurement.dossier.application.service.command.checkAnsweredOnce
import com.procurement.dossier.application.service.command.checkArrays
import com.procurement.dossier.application.service.command.checkAwardCriteriaDetailsAreRequired
import com.procurement.dossier.application.service.command.checkAwardCriteriaDetailsEnum
import com.procurement.dossier.application.service.command.checkAwardCriteriaEnum
import com.procurement.dossier.application.service.command.checkCastCoefficient
import com.procurement.dossier.application.service.command.checkCoefficient
import com.procurement.dossier.application.service.command.checkCoefficientDataType
import com.procurement.dossier.application.service.command.checkCoefficientRelatedOption
import com.procurement.dossier.application.service.command.checkCoefficientValueUniqueness
import com.procurement.dossier.application.service.command.checkConversionRelatesToEnum
import com.procurement.dossier.application.service.command.checkConversionRelation
import com.procurement.dossier.application.service.command.checkConversionWithoutCriteria
import com.procurement.dossier.application.service.command.checkCriteriaAndConversionAreRequired
import com.procurement.dossier.application.service.command.checkCriteriaWithAwardCriteria
import com.procurement.dossier.application.service.command.checkDataTypeValue
import com.procurement.dossier.application.service.command.checkDatatypeCompliance
import com.procurement.dossier.application.service.command.checkDateTime
import com.procurement.dossier.application.service.command.checkIdsUniqueness
import com.procurement.dossier.application.service.command.checkMinMaxValue
import com.procurement.dossier.application.service.command.checkPeriod
import com.procurement.dossier.application.service.command.checkRequirementRelationRelevance
import com.procurement.dossier.application.service.command.checkRequirements
import com.procurement.dossier.application.service.command.createCnEntity
import com.procurement.dossier.application.service.command.extractCreatedCriteria
import com.procurement.dossier.application.service.command.toEntity
import com.procurement.dossier.application.service.context.CheckResponsesContext
import com.procurement.dossier.application.service.context.CreateCriteriaContext
import com.procurement.dossier.application.service.context.GetCriteriaContext
import com.procurement.dossier.infrastructure.converter.createResponseData
import com.procurement.dossier.infrastructure.converter.toData
import com.procurement.dossier.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource
import com.procurement.dossier.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.dossier.infrastructure.model.dto.request.CreateCriteriaRequest
import com.procurement.dossier.infrastructure.utils.toObject

class CriteriaService(
    private val generationService: Generable,
    private val criteriaRepository: CriteriaRepository,
    private val medeiaValidationService: JsonValidationService
) {

    fun createCriteria(cm: CommandMessage, context: CreateCriteriaContext): CreatedCriteria {
        val request: CreateCriteriaRequest = toObject(CreateCriteriaRequest::class.java, cm.data)
        val requestData = request.toData()
        val createdCriteria = buildCriteria(requestData, generationService)
        val createdCriteriaEntity = createdCriteria.toEntity()
        val cn = createCnEntity(createdCriteriaEntity, context)
        criteriaRepository.save(cn)
        return createdCriteria
    }

    fun checkCriteria(cm: CommandMessage) {
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
            .checkCoefficientRelatedOption()   // FReq-1.1.1.28
            .checkConversionRelatesToEnum()    // FReq-1.1.1.14
            .checkAwardCriteriaEnum()          // FReq-1.1.1.15
            .checkAwardCriteriaDetailsEnum()   // FReq-1.1.1.15
            .checkArrays()                     // FReq-1.1.1.16
    }

    fun checkResponses(cm: CommandMessage, context: CheckResponsesContext) {
        val request = medeiaValidationService.validateResponses(cm)
        val cnEntity = criteriaRepository.findBy(context.cpid)
            ?: throw ErrorException(
                error = ErrorType.ENTITY_NOT_FOUND,
                message = "Cannot found record with cpid=${context.cpid}."
            )
        val createdCriteria = cnEntity.extractCreatedCriteria()
            .toData()

        if (createdCriteria.criteria != null && request.bid.requirementResponses == null)
            throw ErrorException(
                error = ErrorType.INVALID_REQUIREMENT_RESPONSE,
                message = "No requirement responses found for requirement from criteria."
            )

        request
            .toData()
            .checkRequirementRelationRelevance(createdCriteria) // FReq-1.2.1.1
            .checkAnswerCompleteness(createdCriteria)           // FReq-1.2.1.2
            .checkAnsweredOnce()                                // FReq-1.2.1.3
            .checkDataTypeValue(createdCriteria)                // FReq-1.2.1.4
            .checkPeriod()                                      // FReq-1.2.1.5 & FReq-1.2.1.7
            .checkIdsUniqueness()                               // FReq-1.2.1.6
    }

    fun getCriteriaDetails(context: GetCriteriaContext): GetCriteriaData? {
        return criteriaRepository.findBy(context.cpid)
            ?.let { cnEntity ->
                cnEntity.extractCreatedCriteria()
                    .toData()
                    .let { createResponseData(it) }
            }
    }

    fun createRequestsForEvPanels(): RequestsForEvPanelsData {
        return RequestsForEvPanelsData(
            criteria = RequestsForEvPanelsData.Criteria(
                id = generationService.generatePermanentCriteriaId(),
                title = "",
                description = "",
                source = CriteriaSource.PROCURING_ENTITY,
                relatesTo = CriteriaRelatesTo.AWARD,
                requirementGroups = listOf(
                    RequestsForEvPanelsData.Criteria.RequirementGroup(
                        id = generationService.generatePermanentRequirementGroupId(),
                        requirements = listOf(
                            Requirement(
                                id = generationService.generatePermanentRequirementId().toString(),
                                title = "",
                                dataType = RequirementDataType.BOOLEAN,
                                value = NoneValue,
                                period = null,
                                description = null
                            )
                        )
                    )
                )
            )
        )
    }
}
