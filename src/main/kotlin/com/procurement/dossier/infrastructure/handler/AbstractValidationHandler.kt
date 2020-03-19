package com.procurement.dossier.infrastructure.handler

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Action
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.dto.ApiSuccessResponse2
import com.procurement.dossier.infrastructure.model.dto.bpe.getId
import com.procurement.dossier.infrastructure.model.dto.bpe.getVersion
import com.procurement.dossier.infrastructure.utils.toJson

abstract class AbstractValidationHandler<ACTION : Action>(
    private val logger: Logger
) : AbstractHandler<ACTION, ApiResponse2>(logger) {

    override fun handle(node: JsonNode): ApiResponse2 {
        val id = node.getId().get
        val version = node.getVersion().get

        val validationResult = execute(node)
            .also {
                logger.info("The '${action.key}' has been executed. Result: '${toJson(it)}'")
            }

        return when (validationResult) {
            is ValidationResult.Ok -> ApiSuccessResponse2(version = version, id = id)
            is ValidationResult.Fail -> responseError(id = id, version = version, fail = validationResult.error)
        }
    }

    abstract fun execute(node: JsonNode): ValidationResult<Fail>
}
