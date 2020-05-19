package com.procurement.dossier.infrastructure.handler.submission

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.handler.AbstractValidationHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams

import com.procurement.dossier.infrastructure.model.dto.request.submission.ValidateSubmissionRequest
import org.springframework.stereotype.Component

@Component
class ValidateSubmissionHandler(logger: Logger) : AbstractValidationHandler<Command2Type>(logger) {

    override val action: Command2Type = Command2Type.VALIDATE_SUBMISSION

    override fun execute(node: JsonNode): ValidationResult<Fail> {
        node.tryGetParams(ValidateSubmissionRequest::class.java)
            .doReturn { error -> return ValidationResult.error(error) }
            .convert()
            .doReturn { error -> return ValidationResult.error(error) }

        return ValidationResult.ok()
    }
}