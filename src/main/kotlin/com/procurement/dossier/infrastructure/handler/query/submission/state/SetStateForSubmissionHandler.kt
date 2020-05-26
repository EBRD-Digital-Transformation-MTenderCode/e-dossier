package com.procurement.dossier.infrastructure.handler.query.submission.state

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionResult
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.converter.submission.state.convert
import com.procurement.dossier.infrastructure.handler.AbstractQueryHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.state.SetStateForSubmissionRequest
import org.springframework.stereotype.Component

@Component
class SetStateForSubmissionHandler(logger: Logger, private val submissionService: SubmissionService) :
    AbstractQueryHandler<Command2Type, SetStateForSubmissionResult>(logger) {

    override val action: Command2Type = Command2Type.SET_STATE_FOR_SUBMISSION

    override fun execute(node: JsonNode): Result<SetStateForSubmissionResult, Fail> {
        val params = node.tryGetParams(SetStateForSubmissionRequest::class.java)
            .orForwardFail { fail -> return fail }
            .convert()
            .orForwardFail { fail -> return fail }

        return submissionService.setStateForSubmission(params)
    }
}