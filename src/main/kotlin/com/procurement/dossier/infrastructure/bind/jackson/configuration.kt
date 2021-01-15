package com.procurement.dossier.infrastructure.bind.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.procurement.dossier.infrastructure.bind.apiversion.ApiVersionDeserializer
import com.procurement.dossier.infrastructure.bind.apiversion.ApiVersionSerializer
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueModule
import com.procurement.dossier.infrastructure.bind.databinding.IntDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.dossier.infrastructure.bind.databinding.StringsDeserializer
import com.procurement.dossier.infrastructure.dto.ApiVersion
import java.time.LocalDateTime

fun ObjectMapper.configuration() {
    val module = SimpleModule().apply {
        /**
         * Serializer/Deserializer for LocalDateTime type
         */
        addSerializer(LocalDateTime::class.java, JsonDateTimeSerializer())
        addDeserializer(LocalDateTime::class.java, JsonDateTimeDeserializer())

        addDeserializer(String::class.java, StringsDeserializer())
        addDeserializer(Int::class.java, IntDeserializer())

        addSerializer(ApiVersion::class.java, ApiVersionSerializer())
        addDeserializer(ApiVersion::class.java, ApiVersionDeserializer())
    }

    this.registerModule(module)
    this.registerModule(KotlinModule())
    this.registerModule(RequirementValueModule())
    this.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
    this.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    this.nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
}
