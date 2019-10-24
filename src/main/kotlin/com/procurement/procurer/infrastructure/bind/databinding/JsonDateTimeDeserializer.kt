package com.procurement.procurer.infrastructure.bind.databinding

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.LocalDateTime

class JsonDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    companion object {
        fun deserialize(value: String): LocalDateTime = LocalDateTime.parse(
            value,
            JsonDateTimeFormatter.formatter
        )
    }

    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): LocalDateTime =
        deserialize(
            jsonParser.text
        )
}
