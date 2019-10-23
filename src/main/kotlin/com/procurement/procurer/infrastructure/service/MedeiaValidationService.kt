package com.procurement.procurer.infrastructure.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.service.JsonValidationService
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.cn.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.utils.toJson
import com.worldturner.medeia.api.UrlSchemaSource
import com.worldturner.medeia.api.jackson.MedeiaJacksonApi

class MedeiaValidationService(
    private val objectMapper: ObjectMapper
) : JsonValidationService {

    private val api = MedeiaJacksonApi()
    private val source = UrlSchemaSource(javaClass.getResource("/json/criteria/check/criteria_schema.json"))
    private val validator = api.loadSchema(source)

    override fun validateViaJsonSchema(cm: CommandMessage): CheckCriteriaRequest =
        try {
            val unvalidatedParser = objectMapper.factory.createParser(toJson(cm.data))
            val validatedParser = api.decorateJsonParser(validator, unvalidatedParser)
            objectMapper.readValue(validatedParser, CheckCriteriaRequest::class.java)
        } catch (exception: Exception) {
            exception.printStackTrace()
            throw ErrorException(
                error = ErrorType.INVALID_JSON,
                message = "Cannot validate json via json schema"
            )
        }
}