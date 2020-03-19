package com.procurement.dossier.infrastructure.service

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.model.dto.bpe.errorResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.getAction
import com.procurement.dossier.infrastructure.model.dto.bpe.getId
import com.procurement.dossier.infrastructure.model.dto.bpe.getVersion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Command2Service {

    companion object {
        private val log = LoggerFactory.getLogger(Command2Service::class.java)
    }

    fun execute(request: JsonNode): ApiResponse2 {

        val version = request.getVersion()
            .doOnError { versionError ->
                val id = request.getId()
                    .doOnError { idError -> return errorResponse(fail = versionError) }
                    .get
                return errorResponse(fail = versionError, id = id)
            }
            .get

        val id = request.getId()
            .doOnError { error -> return errorResponse(fail = error, version = version) }
            .get

        val action = request.getAction()
            .doOnError { error -> return errorResponse(id = id, version = version, fail = error) }
            .get

        val response: ApiResponse2 = when (action) {
            else -> TODO()
        }

        if (log.isDebugEnabled)
            log.debug("DataOfResponse: '$response'.")

        return response
    }
}
