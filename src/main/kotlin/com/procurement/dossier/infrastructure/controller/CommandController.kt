package com.procurement.dossier.infrastructure.controller

import com.procurement.dossier.infrastructure.config.properties.GlobalProperties
import com.procurement.dossier.infrastructure.dto.ApiResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.dossier.infrastructure.model.dto.bpe.errorResponseDto
import com.procurement.dossier.infrastructure.service.CommandService
import com.procurement.dossier.infrastructure.utils.toJson
import com.procurement.dossier.infrastructure.utils.toObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/command")
class CommandController(private val commandService: CommandService) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(CommandController::class.java)
    }

    @PostMapping
    fun command(@RequestBody requestBody: String): ResponseEntity<ApiResponse> {
        if (log.isDebugEnabled)
            log.debug("RECEIVED COMMAND: '$requestBody'.")

        val cm: CommandMessage = try {
            toObject(CommandMessage::class.java, requestBody)
        } catch (expected: Exception) {
            log.debug("Error.", expected)
            val response = errorResponseDto(exception = expected, id = "N/A", version = GlobalProperties.App.apiVersion)
            return ResponseEntity(response, HttpStatus.OK)
        }

        val response = try {
            commandService.execute(cm)
                .also { response ->
                    if (log.isDebugEnabled)
                        log.debug("RESPONSE (operation-id: '${cm.context.operationId}'): '${toJson(response)}'.")
                }
        } catch (expected: Exception) {
            log.debug("Error.", expected)
            errorResponseDto(exception = expected, id = cm.id, version = cm.version)
        }
        return ResponseEntity(response, HttpStatus.OK)
    }
}
