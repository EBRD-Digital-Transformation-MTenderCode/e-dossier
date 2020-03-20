package com.procurement.dossier.domain.model

import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.model.country.CountryId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.utils.toMilliseconds
import java.time.LocalDateTime

class Cpid private constructor(private val value: String) {

    override fun equals(other: Any?): Boolean {
        return if (this !== other)
            other is Cpid
                && this.value == other.value
        else
            true
    }

    override fun hashCode(): Int = value.hashCode()

    @JsonValue
    override fun toString(): String = value

    companion object {
        private val regex = "^[a-z]{4}-[a-z0-9]{6}-[A-Z]{2}-[0-9]{13}\$".toRegex()

        val pattern: String
            get() = regex.pattern

        fun tryCreateOrNull(value: String): Cpid? = if (value.matches(regex)) Cpid(value = value) else null

        fun tryCreate(value: String): Result<Cpid, String> =
            if (value.matches(regex)) Result.success(Cpid(value = value))
            else Result.failure(pattern)

        fun generate(prefix: String, country: CountryId, timestamp: LocalDateTime): Cpid =
            Cpid("${prefix.toLowerCase()}-${country.toUpperCase()}-${timestamp.toMilliseconds()}")
    }
}
