package com.procurement.dossier.infrastructure.bind.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.procurement.dossier.infrastructure.bind.apiversion.ApiVersionModule
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueModule
import com.procurement.dossier.infrastructure.bind.databinding.IntDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeModule
import com.procurement.dossier.infrastructure.bind.databinding.StringsDeserializer

fun ObjectMapper.configuration() {
    val module = SimpleModule().apply {
        addDeserializer(String::class.java, StringsDeserializer())
        addDeserializer(Int::class.java, IntDeserializer())
    }

    this.registerModule(module)
    this.registerModule(KotlinModule())
    this.registerModule(ApiVersionModule())
    this.registerModule(JsonDateTimeModule())
    this.registerModule(RequirementValueModule())

    this.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
    this.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    this.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true)
    this.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false)
    this.configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false)
    this.nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
}
