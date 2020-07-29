package com.procurement.dossier.domain.model.submission

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.*

class SubmissionId private constructor(private val value: String) : Serializable {

    companion object {

        @JvmStatic
        @JsonCreator
        fun create(text: String): SubmissionId = SubmissionId(text)

        fun generate(): SubmissionId = SubmissionId(UUID.randomUUID().toString())
    }

    override fun equals(other: Any?): Boolean {
        return if (this !== other)
            other is SubmissionId
                && this.value == other.value
        else
            true
    }

    override fun hashCode(): Int = value.hashCode()

    @JsonValue
    override fun toString(): String = value
}
