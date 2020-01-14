package com.procurement.procurer.infrastructure.controller

import com.procurement.procurer.application.exception.EnumException
import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.ResponseDto
import com.procurement.procurer.infrastructure.model.dto.bpe.getEnumExceptionResponseDto
import com.procurement.procurer.infrastructure.model.dto.bpe.getErrorExceptionResponseDto
import com.procurement.procurer.infrastructure.model.dto.bpe.getExceptionResponseDto
import com.procurement.procurer.infrastructure.service.CommandService
import com.procurement.procurer.infrastructure.utils.toJson
import com.procurement.procurer.infrastructure.utils.toObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/command")
class CommandController(private val commandService: CommandService) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(CommandController::class.java)
    }

    @PostMapping
    fun command(@RequestBody requestBody: String): ResponseEntity<ResponseDto> {
        if (log.isDebugEnabled)
            log.debug("RECEIVED COMMAND: '$requestBody'.")
        val cm: CommandMessage = toObject(
            CommandMessage::class.java,
            requestBody
        )

        val response = commandService.execute(cm)
        if (log.isDebugEnabled)
            log.debug(
                "RESPONSE (operation-id: '${cm.context.operationId}'): '${toJson(
                    response
                )}'."
            )
        return ResponseEntity(response, HttpStatus.OK)
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception::class)
    fun exception(ex: Exception): ResponseDto {
        log.error("Error", ex)
        return when (ex) {
            is ErrorException -> getErrorExceptionResponseDto(ex)
            is EnumException  -> getEnumExceptionResponseDto(ex)
            else              -> getExceptionResponseDto(ex)
        }
    }
}

