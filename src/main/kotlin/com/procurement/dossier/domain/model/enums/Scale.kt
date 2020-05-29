package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class Scale (@JsonValue override val key: String) : EnumElementProvider.Key {

    MICRO("micro"),
    SME("sme"),
    LARGE("large");

    override fun toString(): String = key

    companion object : EnumElementProvider<Scale>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = Scale.orThrow(name)
    }
}
