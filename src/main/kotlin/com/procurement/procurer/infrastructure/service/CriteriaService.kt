package com.procurement.procurer.infrastructure.service

import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.repository.CriteriaRepository
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.ResponseDto
import com.procurement.procurer.infrastructure.model.dto.cn.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.cn.CreateCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.cn.toData
import com.procurement.procurer.infrastructure.model.entity.CreatedCriteriaEntity
import com.procurement.procurer.infrastructure.service.command.checkActualItemRelation
import com.procurement.procurer.infrastructure.service.command.checkArrays
import com.procurement.procurer.infrastructure.service.command.checkAwardCriteriaDetailsAreRequired
import com.procurement.procurer.infrastructure.service.command.checkAwardCriteriaDetailsEnum
import com.procurement.procurer.infrastructure.service.command.checkAwardCriteriaEnum
import com.procurement.procurer.infrastructure.service.command.checkCastCoefficient
import com.procurement.procurer.infrastructure.service.command.checkCoefficient
import com.procurement.procurer.infrastructure.service.command.checkCoefficientDataType
import com.procurement.procurer.infrastructure.service.command.checkConversionRelatesToEnum
import com.procurement.procurer.infrastructure.service.command.checkConversionRelation
import com.procurement.procurer.infrastructure.service.command.checkConversionWithoutCriteria
import com.procurement.procurer.infrastructure.service.command.checkCriteriaAndConversionAreRequired
import com.procurement.procurer.infrastructure.service.command.checkDatatypeCompliance
import com.procurement.procurer.infrastructure.service.command.checkDateTime
import com.procurement.procurer.infrastructure.service.command.checkMinMaxValue
import com.procurement.procurer.infrastructure.service.command.checkRequirements
import com.procurement.procurer.infrastructure.service.command.createCnEntity
import com.procurement.procurer.infrastructure.service.command.generateCreateCriteriaResponse
import com.procurement.procurer.infrastructure.service.command.processCriteria
import com.procurement.procurer.infrastructure.service.command.toEntity
import com.procurement.procurer.infrastructure.utils.toObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CriteriaService(
    val generationService: GenerationService,
    val criteriaRepository: CriteriaRepository
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CriteriaService::class.java)
    }

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
        val cn = createCnEntity(
            createdCriteriaEntity,
            requestData,
            context
        )

        val wasApplied = criteriaRepository.save(cn)
        val cnEntity = if (!wasApplied) criteriaRepository.findBy(context.cpid) else cn

        val cnResponse = toObject(CreatedCriteriaEntity::class.java, cnEntity!!.jsonData)
        val responseData = generateCreateCriteriaResponse(
            cnResponse
        )

        return ResponseDto(id = cm.id, data = responseData)
    }

    fun checkCriteria(cm: CommandMessage): ResponseDto {
        val request: CheckCriteriaRequest = toObject(
            CheckCriteriaRequest::class.java,
            cm.data
        )

        request
            .toData()
            .checkConversionWithoutCriteria()
            .checkAwardCriteriaDetailsAreRequired()  // FReq-1.1.1.22
            .checkCriteriaAndConversionAreRequired() // FReq-1.1.1.23

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

