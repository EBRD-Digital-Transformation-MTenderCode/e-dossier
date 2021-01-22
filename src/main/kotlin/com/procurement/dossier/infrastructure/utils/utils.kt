package com.procurement.dossier.infrastructure.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.infrastructure.bind.jackson.configuration
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private object JsonMapper {

    val mapper: ObjectMapper = ObjectMapper().apply { configuration() }
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
