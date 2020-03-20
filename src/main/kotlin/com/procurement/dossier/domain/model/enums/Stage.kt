package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class Stage(@JsonValue override val key: String) : EnumElementProvider.Key {

    AC("AC"),
    EI("EI"),
    EV("EV"),
    FS("FS"),
    NP("NP"),
    PN("PN");

    override fun toString(): String = key

    companion object : EnumElementProvider<Stage>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = orThrow(name)
    }
}
