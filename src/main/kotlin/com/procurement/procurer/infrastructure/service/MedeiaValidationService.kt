package com.procurement.procurer.infrastructure.service

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.service.JsonValidationService
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.cn.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.cn.CheckResponsesRequest
import com.procurement.procurer.infrastructure.utils.toJson
import com.worldturner.medeia.api.UrlSchemaSource
import com.worldturner.medeia.api.ValidationFailedException
import com.worldturner.medeia.api.jackson.MedeiaJacksonApi
import java.lang.Exception

class MedeiaValidationService(
    private val objectMapper: ObjectMapper
) : JsonValidationService {

    private val api = MedeiaJacksonApi()
    private val criteriaSchema = UrlSchemaSource(javaClass.getResource("/json/criteria/check/check_criteria_schema.json"))
    private val responsesSchema = UrlSchemaSource(javaClass.getResource("/json/criteria/responses/check_responses_schema.json"))

    private val criteriaValidator = api.loadSchema(criteriaSchema)
    private val responsesValidator = api.loadSchema(responsesSchema)

    override fun validateCriteria(cm: CommandMessage): CheckCriteriaRequest =
        try {
            val unvalidatedParser = objectMapper.factory.createParser(toJson(cm.data))
            val validatedParser = api.decorateJsonParser(criteriaValidator, unvalidatedParser)
            objectMapper.readValue(validatedParser, CheckCriteriaRequest::class.java)
        } catch (exception: Exception) {
            errorHandling(exception)
        }

    override fun validateResponses(cm: CommandMessage): CheckResponsesRequest =
        try {
            val unvalidatedParser = objectMapper.factory.createParser(toJson(cm.data))
            val validatedParser = api.decorateJsonParser(responsesValidator, unvalidatedParser)
            objectMapper.readValue(validatedParser, CheckResponsesRequest::class.java)
        } catch (exception: Exception) {
            errorHandling(exception)
        }


    private fun errorHandling(exception: Exception): Nothing {
        when (exception) {
            is ValidationFailedException -> processingJsonSchemaException(exception = exception)
            is JsonMappingException -> {
                val cause = exception.cause ?: exception
                if (cause is ValidationFailedException) { processingJsonSchemaException(exception = cause) }

                throw ErrorException(
                    error = ErrorType.INVALID_JSON,
                    message = "Cannot validate json via json schema. ${(exception.message)}"
                )
            }
            else -> {
                throw ErrorException(
                    error = ErrorType.INVALID_JSON,
                    message = "Cannot validate json via json schema. ${(exception.message)}"
                )
            }
        }
    }

    private fun processingJsonSchemaException(exception: ValidationFailedException): Nothing {
        val errorDetails = StringBuilder()
        var delails = exception.failures[0].details
        if (exception.failures[0].property != null)  errorDetails.append(exception.failures[0].property + " -> ")
        while (!delails.isEmpty()) {
            val fail = delails.toMutableList()[0]

            if (fail.property != null)  errorDetails.append(fail.property + " -> ")
            if (delails.toMutableList().get(0).details.isEmpty()) errorDetails.append(fail.message)

            delails = delails.toMutableList().get(0).details
        }
        throw ErrorException(
            error = ErrorType.INVALID_JSON,
            message = errorDetails.toString()
        )
    }
}