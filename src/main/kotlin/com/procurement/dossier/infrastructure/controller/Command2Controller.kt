package com.procurement.dossier.infrastructure.controller

import com.datastax.driver.core.querybuilder.QueryBuilder.toJson
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.BadRequestErrors
import com.procurement.dossier.infrastructure.config.properties.GlobalProperties
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.dto.ApiVersion
import com.procurement.dossier.infrastructure.model.dto.bpe.NaN
import com.procurement.dossier.infrastructure.model.dto.bpe.errorResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.getId
import com.procurement.dossier.infrastructure.model.dto.bpe.getVersion
import com.procurement.dossier.infrastructure.service.Command2Service
import com.procurement.dossier.infrastructure.utils.toNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/command2")
class Command2Controller(
    private val command2Service: Command2Service,
    private val logger: Logger
) {

    @PostMapping
    fun command(@RequestBody requestBody: String): ResponseEntity<ApiResponse2> {

        logger.info("RECEIVED COMMAND: '${requestBody}'.")

        val node = requestBody.toNode()
            .doOnError { error ->
                return responseEntity(
                    expected = BadRequestErrors.Parsing(
                        message = "Invalid request data",
                        request = requestBody
                    )
                )
            }
            .get

        val version = node.getVersion()
            .doOnError { versionError ->
                val id = node.getId()
                    .doOnError { idError -> return responseEntity(expected = versionError) }
                    .get
                return responseEntity(expected = versionError, id = id)
            }
            .get

        val id = node.getId()
            .doOnError { error -> return responseEntity(expected = error, version = version) }
            .get

        val response = command2Service.execute(request = node)
            .also { response ->
                logger.info("RESPONSE (id: '${id}'): '${toJson(response)}'.")
            }
        return ResponseEntity(response, HttpStatus.OK)
    }

    private fun responseEntity(
        expected: Fail,
        id: UUID = NaN,
        version: ApiVersion = GlobalProperties.App.apiVersion
    ): ResponseEntity<ApiResponse2> {
        expected.logging(logger)
        val response = errorResponse(fail = expected, id = id, version = version)
        return ResponseEntity(response, HttpStatus.OK)
    }
}
