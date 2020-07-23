package com.procurement.dossier.infrastructure.handler.query

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.get.GetSubmissionsByQualificationIdsResult
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.handler.AbstractQueryHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.GetSubmissionsByQualificationIdsRequest
import org.springframework.stereotype.Component

@Component
class GetSubmissionsByQualificationIdsHandler(logger: Logger, private val submissionService: SubmissionService) :
    AbstractQueryHandler<Command2Type, GetSubmissionsByQualificationIdsResult>(logger) {

    override val action: Command2Type = Command2Type.GET_SUBMISSIONS_BY_QUALIFICATION_IDS

    override fun execute(node: JsonNode): Result<GetSubmissionsByQualificationIdsResult, Fail> {
        val params = node.tryGetParams(GetSubmissionsByQualificationIdsRequest::class.java)
            .orForwardFail { fail -> return fail }
            .convert()
            .orForwardFail { fail -> return fail }

        return submissionService.getSubmissionsByQualificationIds(params)
    }
}