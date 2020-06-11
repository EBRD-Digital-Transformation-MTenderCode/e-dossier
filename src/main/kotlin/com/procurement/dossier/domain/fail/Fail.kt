package com.procurement.dossier.domain.fail

import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.EnumElementProvider
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.ValidationResult

sealed class Fail {

    abstract val code: String
    abstract val description: String
    val message: String
        get() = "ERROR CODE: '$code', DESCRIPTION: '$description'."

    abstract fun logging(logger: Logger)

    abstract class Error(val prefix: String) : Fail() {
        companion object {
            fun <T, E : Error> E.toResult(): Result<T, E> = Result.failure(this)
            fun <E : Error> E.toValidationResult(): ValidationResult<E> = ValidationResult.error(this)
        }

        override fun logging(logger: Logger) {
            logger.error(message = message)
        }
    }

    sealed class Incident(val level: Level, number: String, override val description: String) : Fail() {
        override val code: String = "INC-$number"

        override fun logging(logger: Logger) {
            when (level) {
                Level.ERROR -> logger.error(message)
                Level.WARNING -> logger.warn(message)
                Level.INFO -> logger.info(message)
            }
        }

        sealed class Database(val number: String, override val description: String) :
            Incident(level = Level.ERROR, number = number, description = description) {

            class Interaction(private val exception: Exception) : Database(
                number = "1.1",
                description = "Database incident."
            ) {
                override fun logging(logger: Logger) {
                    logger.error(message = message, exception = exception)
                }
            }

            class RecordDoesNotExist(override val description: String) : Database(
                number = "1.2",
                description = description
            )

            class Consistency(message: String) : Database(
                number = "1.3",
                description = "Database consistency incident. $message"
            )

            class Parsing(val column: String, val value: String, val exception: Exception? = null) :
                Database(
                    number = "1.4",
                    description = "Could not parse data stored in database."
                ) {
                override fun logging(logger: Logger) {
                    logger.error(message = message, mdc = mapOf("column" to column, "value" to value), exception = exception)
                }
            }
        }

        sealed class Transform(val number: String, override val description: String, val exception: Exception? = null) :
            Incident(level = Level.ERROR, number = number, description = description) {

            override fun logging(logger: Logger) {
                logger.error(message = message, exception = exception)
            }

            class Parsing(className: String, exception: Exception? = null) :
                Transform(number = "2.1", description = "Error parsing to $className.", exception = exception)

        }

        enum class Level(override val key: String) : EnumElementProvider.Key {
            ERROR("error"),
            WARNING("warning"),
            INFO("info");

            companion object : EnumElementProvider<Level>(info = info())
        }
    }
}