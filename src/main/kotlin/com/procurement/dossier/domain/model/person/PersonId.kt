package com.procurement.dossier.domain.model.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.Result.Companion.success

class PersonId private constructor(val value: String) {

    @JsonValue
    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = if (this !== other)
        other is PersonId
            && this.value == other.value
    else
        true

    override fun hashCode(): Int = value.hashCode()

    companion object {

        @JvmStatic
        @JsonCreator
        fun parse(text: String): PersonId? = if (text.isBlank())
            null
        else
            PersonId(text)

        fun tryCreate(text: String): Result<PersonId, DataErrors> =
            if (text.isBlank())
                failure(DataErrors.Validation.EmptyString(name = "id"))
            else
                success(PersonId(text))

        fun generate(scheme: String, id: String): PersonId = PersonId("$scheme-$id")
    }
}
