package com.procurement.dossier.domain.util.extension

const val UUID_PATTERN: String = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"

val UUIDRegex = UUID_PATTERN.toRegex()

fun String.isUUID(): Boolean = UUIDRegex.matches(this)
