package com.procurement.dossier.infrastructure.service

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.handler.historical.submission.CreateSubmissionHandler
import com.procurement.dossier.infrastructure.handler.historical.submission.SetStateForSubmissionHandler
import com.procurement.dossier.infrastructure.handler.query.FindSubmissionsForOpeningHandler
import com.procurement.dossier.infrastructure.handler.query.GetOrganizationsHandler
import com.procurement.dossier.infrastructure.handler.query.GetSubmissionPeriodEndDateHandler
import com.procurement.dossier.infrastructure.handler.query.GetSubmissionStateByIdsHandler
import com.procurement.dossier.infrastructure.handler.validate.period.CheckPeriod2Handler
import com.procurement.dossier.infrastructure.handler.validate.requirementresponse.ValidateRequirementResponseHandler
import com.procurement.dossier.infrastructure.handler.validate.submission.CheckAccessToSubmissionHandler
import com.procurement.dossier.infrastructure.handler.validate.submission.ValidateSubmissionHandler
import com.procurement.dossier.infrastructure.handler.verify.submissionperiodend.VerifySubmissionPeriodEndHandler
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
    private val checkPeriod2Handler: CheckPeriod2Handler,
    private val getSubmissionStateByIdsHandler: GetSubmissionStateByIdsHandler,
    private val setStateForSubmissionHandler: SetStateForSubmissionHandler,
    private val checkAccessToSubmissionHandler: CheckAccessToSubmissionHandler,
    private val verifySubmissionPeriodEndHandler: VerifySubmissionPeriodEndHandler,
    private val checkAccessToSubmissionHandler: CheckAccessToSubmissionHandler,
    private val getOrganizationsHandler: GetOrganizationsHandler,
    private val getSubmissionPeriodEndDateHandler: GetSubmissionPeriodEndDateHandler,
    private val findSubmissionsForOpeningHandler: FindSubmissionsForOpeningHandler
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
            Command2Type.GET_SUBMISSION_STATE_BY_IDS -> getSubmissionStateByIdsHandler.handle(node = request)
            Command2Type.SET_STATE_FOR_SUBMISSION -> setStateForSubmissionHandler.handle(node = request)
            Command2Type.CHECK_ACCESS_TO_SUBMISSION -> checkAccessToSubmissionHandler.handle(node = request)
            Command2Type.VERIFY_SUBMISSION_PERIOD_END -> verifySubmissionPeriodEndHandler.handle(node = request)
            Command2Type.GET_ORGANIZATIONS -> getOrganizationsHandler.handle(node = request)
            Command2Type.GET_SUBMISSION_PERIOD_END_DATE -> getSubmissionPeriodEndDateHandler.handle(node = request)
            Command2Type.FIND_SUBMISSIONS_FOR_OPENING -> findSubmissionsForOpeningHandler.handle(node = request)
        }

        logger.info("DataOfResponse: '$response'.")

        return response
    }
}
