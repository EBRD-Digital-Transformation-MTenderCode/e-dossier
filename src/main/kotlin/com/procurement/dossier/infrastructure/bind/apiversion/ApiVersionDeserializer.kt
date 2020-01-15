package com.procurement.dossier.infrastructure.bind.apiversion

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.procurement.dossier.infrastructure.dto.ApiVersion
import java.io.IOException

class ApiVersionDeserializer : JsonDeserializer<ApiVersion>() {
    companion object {
        fun deserialize(text: String) = ApiVersion.valueOf(text)
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ApiVersion =
        deserialize(jsonParser.text)
}