package com.procurement.dossier.infrastructure.handler.query

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.get.GetInvitedCandidatesOwnersResult
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.bind
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.handler.AbstractQueryHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.GetInvitedCandidatesOwnersRequest
import org.springframework.stereotype.Component

@Component
class GetInvitedCandidatesOwnersHandler(logger: Logger, private val submissionService: SubmissionService) :
    AbstractQueryHandler<Command2Type, GetInvitedCandidatesOwnersResult>(logger) {

    override val action: Command2Type = Command2Type.GET_INVITED_CANDIDATES_OWNERS

    override fun execute(node: JsonNode): Result<GetInvitedCandidatesOwnersResult, Fail> {
        val params = node.tryGetParams(GetInvitedCandidatesOwnersRequest::class.java)
            .bind { it.convert() }
            .orForwardFail { fail -> return fail }

        return submissionService.getInvitedCandidatesOwners(params)
    }
}