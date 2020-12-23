package com.procurement.dossier.infrastructure.handler.historical.submission

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionResult
import com.procurement.dossier.application.repository.history.HistoryRepository
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.bind
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.handler.AbstractHistoricalHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.CreateSubmissionRequest
import org.springframework.stereotype.Component

@Component
class CreateSubmissionHandler(
    logger: Logger,
    historyRepository: HistoryRepository,
    private val submissionService: SubmissionService
) :
    AbstractHistoricalHandler<Command2Type, CreateSubmissionResult>(
        logger = logger,
        historyRepository = historyRepository,
        target = CreateSubmissionResult::class.java
    ) {

    override val action: Command2Type = Command2Type.CREATE_SUBMISSION

    override fun execute(node: JsonNode): Result<CreateSubmissionResult, Fail> {
        val data = node.tryGetParams(CreateSubmissionRequest::class.java)
            .bind { it.convert() }
            .orForwardFail { error -> return error }

        return submissionService.createSubmission(data)
    }
}
