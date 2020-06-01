package com.procurement.dossier.infrastructure.handler.query

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.period.get.GetSubmissionPeriodEndDateResult
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.PeriodService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.converter.period.convert
import com.procurement.dossier.infrastructure.handler.AbstractQueryHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.period.GetSubmissionPeriodEndDateRequest
import org.springframework.stereotype.Component

@Component
class GetSubmissionPeriodEndDateHandler(logger: Logger, private val periodService: PeriodService) :
    AbstractQueryHandler<Command2Type, GetSubmissionPeriodEndDateResult>(logger) {

    override val action: Command2Type = Command2Type.GET_SUBMISSION_PERIOD_END_DATE

    override fun execute(node: JsonNode): Result<GetSubmissionPeriodEndDateResult, Fail> {
        val params = node.tryGetParams(GetSubmissionPeriodEndDateRequest::class.java)
            .orForwardFail { fail -> return fail }
            .convert()
            .orForwardFail { fail -> return fail }

        return periodService.getSubmissionPeriodEndDate(params)
    }
}