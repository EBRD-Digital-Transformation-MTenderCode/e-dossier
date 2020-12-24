package com.procurement.dossier.domain.util.extension

inline fun <T : String?, E : RuntimeException> T.takeIfNotEmpty(error: () -> E): T =
    if (this != null && this.isBlank()) throw error() else this

fun <T> T?.takeIfNotNullOrDefault(default: T?): T? = this ?: default

inline fun <T : String?> T.errorIfBlank(error: () -> java.lang.RuntimeException): T =
    if (this != null && this.isBlank()) throw error() else this
