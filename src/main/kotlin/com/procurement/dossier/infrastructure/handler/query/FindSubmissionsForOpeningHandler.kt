package com.procurement.dossier.infrastructure.handler.query

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsForOpeningResult
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.handler.AbstractQueryHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.FindSubmissionsForOpeningRequest
import org.springframework.stereotype.Component

@Component
class FindSubmissionsForOpeningHandler(logger: Logger, private val submissionService: SubmissionService) :
    AbstractQueryHandler<Command2Type, List<FindSubmissionsForOpeningResult>>(logger) {

    override val action: Command2Type = Command2Type.FIND_SUBMISSIONS

    override fun execute(node: JsonNode): Result<List<FindSubmissionsForOpeningResult>, Fail> {
        val params = node.tryGetParams(FindSubmissionsForOpeningRequest::class.java)
            .orForwardFail { fail -> return fail }
            .convert()
            .orForwardFail { fail -> return fail }

        return submissionService.findSubmissionsForOpening(params)
    }
}