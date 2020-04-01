package com.procurement.dossier.domain.model

import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.model.enums.Stage
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.utils.toMilliseconds

import java.time.LocalDateTime

class Ocid private constructor(private val value: String) {

    override fun equals(other: Any?): Boolean {
        return if (this !== other)
            other is Ocid
                && this.value == other.value
        else
            true
    }

    override fun hashCode(): Int = value.hashCode()

    @JsonValue
    override fun toString(): String = value

    companion object {
        private val STAGES: String
            get() = Stage.allowedValues.joinToString(separator = "|", prefix = "(", postfix = ")") { it.toUpperCase() }

        private val regex = "^[a-z]{4}-[a-z0-9]{6}-[A-Z]{2}-[0-9]{13}-$STAGES-[0-9]{13}\$".toRegex()

        val pattern: String
            get() = regex.pattern

        fun tryCreateOrNull(value: String): Ocid? = if (value.matches(regex)) Ocid(value = value) else null

        fun tryCreate(value: String): Result<Ocid, String> =
            if (value.matches(Ocid.regex)) Result.success(Ocid(value = value))
            else Result.failure(Ocid.pattern)

        fun generate(cpid: Cpid, stage: Stage, timestamp: LocalDateTime): Ocid =
            Ocid("$cpid-$stage-${timestamp.toMilliseconds()}")
    }
}
