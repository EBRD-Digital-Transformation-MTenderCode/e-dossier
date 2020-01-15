package com.procurement.dossier.application.exception

class ErrorException(error: ErrorType, message: String? = null) : RuntimeException(
    when (message) {
        null -> error.message
        else -> error.message + " " + message
    }
) {
    val code: String = error.code
}
