package com.procurement.dossier.infrastructure.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.infrastructure.bind.databinding.IntDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.dossier.infrastructure.bind.databinding.StringsDeserializer
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private object JsonMapper {

    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    init {
        val module = SimpleModule()
        module.addSerializer(LocalDateTime::class.java,
                             JsonDateTimeSerializer()
        )
        module.addDeserializer(LocalDateTime::class.java,
                               JsonDateTimeDeserializer()
        )
        module.addDeserializer(String::class.java,
                               StringsDeserializer()
        )
        module.addDeserializer(Int::class.java,
                               IntDeserializer()
        )

        mapper.registerModule(module)
        mapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true)

        mapper.nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
    }
}

/*Date utils*/
fun localNowUTC(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
}

fun milliNowUTC(): Long {
    return localNowUTC().toInstant(ZoneOffset.UTC).toEpochMilli()
}

fun LocalDateTime.toMilliseconds(): Long = this.toInstant(ZoneOffset.UTC).toEpochMilli()

/*Json utils*/

fun <Any> toJson(obj: Any): String {
    try {
        return JsonMapper.mapper.writeValueAsString(obj)
    } catch (e: JsonProcessingException) {
        throw RuntimeException(e)
    }
}

fun <Any> tryToJson(obj: Any): Result<String, Fail.Incident.Transform.Parsing>  =
    try {
        JsonMapper.mapper.writeValueAsString(obj).asSuccess()
    } catch (expected: JsonProcessingException) {
        Result.failure(Fail.Incident.Transform.Parsing(className = String::class.java.canonicalName, exception = expected))
    }

fun <T> toObject(clazz: Class<T>, json: String): T {
    try {
        return JsonMapper.mapper.readValue(json, clazz)
    } catch (e: IOException) {
        throw IllegalArgumentException(e)
    }
}

fun <T> toObject(clazz: Class<T>, json: JsonNode): T {
    try {
        return JsonMapper.mapper.treeToValue(json, clazz)
    } catch (e: IOException) {
        throw IllegalArgumentException(e)
    }
}

fun <T : Any> JsonNode.tryToObject(target: Class<T>): Result<T, Fail.Incident.Transform.Parsing> = try {
    Result.success(JsonMapper.mapper.treeToValue(this, target))
} catch (expected: Exception) {
    Result.failure(Fail.Incident.Transform.Parsing(className = target.canonicalName, exception = expected))
}

fun <T : Any> String.tryToObject(target: Class<T>): Result<T, Fail.Incident.Transform.Parsing> = try {
    Result.success(JsonMapper.mapper.readValue(this, target))
} catch (expected: Exception) {
    Result.failure(Fail.Incident.Transform.Parsing(className = target.canonicalName, exception = expected))
}

fun String.toNode(): Result<JsonNode, Fail.Incident.Transform.Parsing> = try {
    Result.success(JsonMapper.mapper.readTree(this))
} catch (exception: JsonProcessingException) {
    Result.failure(Fail.Incident.Transform.Parsing(className = JsonNode::class.java.canonicalName, exception = exception))
}
