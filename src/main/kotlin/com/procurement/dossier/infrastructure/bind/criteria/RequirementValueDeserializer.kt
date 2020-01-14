package com.procurement.procurer.infrastructure.bind.criteria

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.procurement.procurer.application.model.data.RequirementRsValue
import com.procurement.procurer.infrastructure.exception.RequirementValueException
import java.io.IOException
import java.math.BigDecimal

class RequirementValueDeserializer : JsonDeserializer<RequirementRsValue>() {
    companion object {
        fun deserialize(value: String): RequirementRsValue = RequirementRsValue.AsString(value)
        fun deserialize(value: Boolean): RequirementRsValue = RequirementRsValue.AsBoolean(value)
        fun deserialize(value: BigDecimal): RequirementRsValue = RequirementRsValue.AsNumber(value)
        fun deserialize(value: Long): RequirementRsValue = RequirementRsValue.AsInteger(value)
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(
        jsonParser: JsonParser,
        deserializationContext: DeserializationContext
    ): RequirementRsValue {
        return when (jsonParser.currentToken) {
            JsonToken.VALUE_STRING       -> deserialize(jsonParser.text)
            JsonToken.VALUE_FALSE        -> deserialize(false)
            JsonToken.VALUE_TRUE         -> deserialize(true)
            JsonToken.VALUE_NUMBER_INT   -> deserialize(jsonParser.longValue)
            JsonToken.VALUE_NUMBER_FLOAT -> deserialize(BigDecimal(jsonParser.text))
            else                         -> throw RequirementValueException(
                requirementValue = jsonParser.text,
                description = "Invalid type"
            )
        }
    }
}
