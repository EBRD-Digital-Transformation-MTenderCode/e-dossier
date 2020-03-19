package com.procurement.dossier.domain.fail.error

import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.fail.Fail

sealed class BadRequestErrors(
    numberError: String,
    override val description: String
) : Fail.Error("BR-") {

    override val code: String = prefix + numberError

    class Parsing(message: String, val request: String) : BadRequestErrors(
        numberError = "01",
        description = message
    ) {
        override fun logging(logger: Logger) {
            logger.error(message = "$message INVALID BODY: '$request'.")
        }
    }

    override fun logging(logger: Logger) {
        logger.error(message = message)
    }
}
