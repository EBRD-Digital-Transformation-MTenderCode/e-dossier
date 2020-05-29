package com.procurement.dossier.domain.util.extension

import com.procurement.dossier.domain.util.Result
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.*

private const val FORMAT_PATTERN = "uuuu-MM-dd'T'HH:mm:ss'Z'"
private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN)
    .withResolverStyle(ResolverStyle.STRICT)

fun LocalDateTime.format(): String = this.format(formatter)

fun LocalDateTime.toDate(): Date {
    return Date.from(this.toInstant(ZoneOffset.UTC))
}

fun Date.toLocal(): LocalDateTime {
    return LocalDateTime.ofInstant(this.toInstant(), ZoneOffset.UTC)
}

fun String.parseLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, formatter)

fun nowDefaultUTC(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

fun String.tryParseLocalDateTime(): Result<LocalDateTime, String> = try {
    Result.success(this.parseLocalDateTime())
} catch (ignore: Exception) {
    Result.failure(FORMAT_PATTERN)
}