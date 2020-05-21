package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class SubmissionStatus (@JsonValue override val key: String) : EnumElementProvider.Key {

    PENDING("pending");

    override fun toString(): String = key

    companion object : EnumElementProvider<SubmissionStatus>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = SubmissionStatus.orThrow(name)
    }
}