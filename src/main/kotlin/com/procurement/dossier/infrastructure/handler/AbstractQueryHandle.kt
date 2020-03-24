package com.procurement.dossier.infrastructure.handler


import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Action
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.dto.ApiSuccessResponse2
import com.procurement.dossier.infrastructure.model.dto.bpe.getId
import com.procurement.dossier.infrastructure.model.dto.bpe.getVersion
import com.procurement.dossier.infrastructure.utils.toJson

abstract class AbstractQueryHandler<ACTION : Action, R : Any>(
    private val logger: Logger
) : AbstractHandler<ACTION, ApiResponse2>(logger) {


    override fun handle(node: JsonNode): ApiResponse2 {
        val id = node.getId().get
        val version = node.getVersion().get

        val result = execute(node)
            .doOnError {error -> return responseError(id = id, version = version, fail = error)  }
            .get

        return ApiSuccessResponse2(id = id, version = version, result = result)
            .also {
                logger.info("The '${action.key}' has been executed. Result: '${toJson(it)}'")
            }
    }

    abstract fun execute(node: JsonNode): Result<R, Fail>
}
