package com.procurement.dossier.infrastructure.handler.validate.period

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.PeriodService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.converter.period.convert
import com.procurement.dossier.infrastructure.handler.AbstractValidationHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.period.CheckPeriod2Request
import org.springframework.stereotype.Component

@Component
class CheckPeriod2Handler(
    private val periodService: PeriodService, logger: Logger
) : AbstractValidationHandler<Command2Type>(logger) {

    override val action: Command2Type = Command2Type.CHECK_PERIOD_2

    override fun execute(node: JsonNode): ValidationResult<Fail> {
        val data = node.tryGetParams(CheckPeriod2Request::class.java)
            .doReturn { error -> return ValidationResult.error(error) }
            .convert()
            .doReturn { error -> return ValidationResult.error(error) }

        return periodService.checkPeriod2(data)
    }
}