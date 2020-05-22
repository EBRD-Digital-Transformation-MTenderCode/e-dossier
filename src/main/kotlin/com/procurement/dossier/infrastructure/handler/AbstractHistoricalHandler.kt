package com.procurement.dossier.infrastructure.handler

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.repository.history.HistoryRepository
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Action
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.dto.ApiSuccessResponse2
import com.procurement.dossier.infrastructure.model.dto.bpe.getId
import com.procurement.dossier.infrastructure.model.dto.bpe.getVersion
import com.procurement.dossier.infrastructure.utils.toJson
import com.procurement.dossier.infrastructure.utils.tryToObject

abstract class AbstractHistoricalHandler<ACTION : Action, R : Any>(
    private val target: Class<R>,
    private val historyRepository: HistoryRepository,
    private val logger: Logger
) : AbstractHandler<ACTION, ApiResponse2>(logger) {

    override fun handle(node: JsonNode): ApiResponse2 {
        val id = node.getId().get
        val version = node.getVersion().get

        val history = historyRepository.getHistory(id.toString(), action.key)
            .doReturn { error -> return responseError(id = id, version = version, fail = error) }

        if (history != null) {
            val data = history.jsonData
            val result = data.tryToObject(target)
                .doReturn { incident ->
                    return responseError(
                        id = id,
                        version = version,
                        fail = Fail.Incident.Transform.ParseFromDatabaseIncident(data, incident.exception)
                    )
                }
            return ApiSuccessResponse2(version = version, id = id, result = result)
        }

        return when (val result = execute(node)) {
            is Result.Success -> {
                val resultData = result.get
                historyRepository.saveHistory(id.toString(), action.key, resultData)
                if (logger.isDebugEnabled)
                    logger.debug("${action.key} has been executed. Result: ${toJson(resultData)}")

                ApiSuccessResponse2(version = version, id = id, result = resultData)
            }
            is Result.Failure -> responseError(
                fail = result.error, version = version, id = id
            )
        }
    }

    abstract fun execute(node: JsonNode): Result<R, Fail>
}

