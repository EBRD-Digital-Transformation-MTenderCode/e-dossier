package com.procurement.dossier.infrastructure.handler.historical.submission

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionResult
import com.procurement.dossier.application.repository.history.HistoryRepository
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.converter.submission.state.convert
import com.procurement.dossier.infrastructure.handler.AbstractHistoricalHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.state.SetStateForSubmissionRequest
import org.springframework.stereotype.Component

@Component
class SetStateForSubmissionHandler(
    logger: Logger,
    historyRepository: HistoryRepository,
    private val submissionService: SubmissionService
) :
    AbstractHistoricalHandler<Command2Type, SetStateForSubmissionResult>(
        logger = logger,
        historyRepository = historyRepository,
        target = SetStateForSubmissionResult::class.java
    ) {

    override val action: Command2Type = Command2Type.SET_STATE_FOR_SUBMISSION

    override fun execute(node: JsonNode): Result<SetStateForSubmissionResult, Fail> {
        val params = node.tryGetParams(SetStateForSubmissionRequest::class.java)
            .orForwardFail { fail -> return fail }
            .convert()
            .orForwardFail { fail -> return fail }

        return submissionService.setStateForSubmission(params)
    }
}