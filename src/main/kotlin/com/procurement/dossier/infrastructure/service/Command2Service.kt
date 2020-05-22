package com.procurement.dossier.infrastructure.service

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.handler.submission.CreateSubmissionHandler
import com.procurement.dossier.infrastructure.handler.validate.period.CheckPeriod2Handler
import com.procurement.dossier.infrastructure.handler.validate.requirementresponse.ValidateRequirementResponseHandler
import com.procurement.dossier.infrastructure.handler.validate.submission.ValidateSubmissionHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.errorResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.getAction
import com.procurement.dossier.infrastructure.model.dto.bpe.getId
import com.procurement.dossier.infrastructure.model.dto.bpe.getVersion
import org.springframework.stereotype.Service

@Service
class Command2Service(
    private val logger: Logger,
    private val validationRequirementResponseHandler: ValidateRequirementResponseHandler,
    private val validateSubmissionHandler: ValidateSubmissionHandler,
    private val createSubmissionHandler: CreateSubmissionHandler,
    private val checkPeriod2Handler: CheckPeriod2Handler
) {

    fun execute(request: JsonNode): ApiResponse2 {

        val version = request.getVersion()
            .doOnError { versionError ->
                val id = request.getId()
                    .doOnError { return errorResponse(fail = versionError) }
                    .get
                return errorResponse(fail = versionError, id = id)
            }
            .get

        val id = request.getId()
            .doOnError { error -> return errorResponse(fail = error, version = version) }
            .get

        val action = request.getAction()
            .doOnError { error -> return errorResponse(id = id, version = version, fail = error) }
            .get

        val response: ApiResponse2 = when (action) {
            Command2Type.VALIDATE_REQUIREMENT_RESPONSE -> validationRequirementResponseHandler.handle(node = request)
            Command2Type.VALIDATE_SUBMISSION -> validateSubmissionHandler.handle(node = request)
            Command2Type.CREATE_SUBMISSION -> createSubmissionHandler.handle(node = request)
            Command2Type.CHECK_PERIOD_2 -> checkPeriod2Handler.handle(node = request)
        }

        logger.info("DataOfResponse: '$response'.")

        return response
    }
}
