package com.procurement.dossier.infrastructure.handler.validate.requirementresponse

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.ValidationService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.converter.convert
import com.procurement.dossier.infrastructure.handler.AbstractValidationHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import org.springframework.stereotype.Service

@Service
class ValidateRequirementResponseHandler(
    logget: Logger,
    private val validationService: ValidationService
) : AbstractValidationHandler<Command2Type>(logger = logget) {

    override fun execute(node: JsonNode): ValidationResult<Fail> {

        val params = node.tryGetParams(ValidateRequirementResponseRequest::class.java)
            .doReturn { error -> return ValidationResult.error(error) }
            .convert()
            .doOnError { error -> return ValidationResult.error(error) }
            .get

        return validationService.validateRequirementResponse(params = params)
    }

    override val action: Command2Type
        get() = Command2Type.VALIDATE_REQUIREMENT_RESPONSE
}
