package com.procurement.dossier.infrastructure.handler.historical.submission

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.finalize.FinalizeSubmissionsResult
import com.procurement.dossier.application.repository.history.HistoryRepository
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.handler.AbstractHistoricalHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.finalize.FinalizeSubmissionsRequest
import org.springframework.stereotype.Component

@Component
class FinalizeSubmissionsHandler(
    logger: Logger,
    historyRepository: HistoryRepository,
    private val submissionService: SubmissionService
) :
    AbstractHistoricalHandler<Command2Type, FinalizeSubmissionsResult>(
        logger = logger,
        historyRepository = historyRepository,
        target = FinalizeSubmissionsResult::class.java
    ) {

    override val action: Command2Type = Command2Type.FINALIZE_SUBMISSIONS

    override fun execute(node: JsonNode): Result<FinalizeSubmissionsResult, Fail> {
        val params = node.tryGetParams(FinalizeSubmissionsRequest::class.java)
            .orForwardFail { error -> return error }
            .convert()
            .orForwardFail { error -> return error }

        return submissionService.finalizeSubmissions(params)
    }
}
