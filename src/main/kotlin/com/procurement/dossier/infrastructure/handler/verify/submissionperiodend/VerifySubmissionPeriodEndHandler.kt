package com.procurement.dossier.infrastructure.handler.verify.submissionperiodend

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.PeriodService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.handler.AbstractQueryHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import org.springframework.stereotype.Service

@Service
class VerifySubmissionPeriodEndHandler(
    logget: Logger,
    private val periodService: PeriodService
) : AbstractQueryHandler<Command2Type, VerifySubmissionPeriodEndResult>(logger = logget) {

    override fun execute(node: JsonNode): Result<VerifySubmissionPeriodEndResult, Fail> {

        val params = node.tryGetParams(VerifySubmissionPeriodEndRequest::class.java)
            .orForwardFail { error -> return error }
            .convert()
            .orForwardFail { error -> return error }

        return periodService.verifySubmissionPeriodEnd(params = params)
    }

    override val action: Command2Type
        get() = Command2Type.VERIFY_SUBMISSION_PERIOD_END
}
