package com.procurement.dossier.infrastructure.handler.historical.submission

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.repository.history.HistoryRepository
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.bind
import com.procurement.dossier.infrastructure.converter.convert
import com.procurement.dossier.infrastructure.handler.AbstractHistoricalHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.PersonesProcessingRequest
import com.procurement.dossier.infrastructure.model.dto.response.PersonesProcessingResponse
import org.springframework.stereotype.Component

@Component
class PersonesProcessingHandler(
    logger: Logger,
    historyRepository: HistoryRepository,
    private val submissionService: SubmissionService
) :
    AbstractHistoricalHandler<Command2Type, PersonesProcessingResponse>(
        logger = logger,
        historyRepository = historyRepository,
        target = PersonesProcessingResponse::class.java
    ) {

    override val action: Command2Type = Command2Type.PERSONES_PROCESSING

    override fun execute(node: JsonNode): Result<PersonesProcessingResponse, Fail> {
        val data = node.tryGetParams(PersonesProcessingRequest::class.java)
            .bind { it.convert() }
            .orForwardFail { error -> return error }

        return submissionService.personesProcessing(data)
    }
}
