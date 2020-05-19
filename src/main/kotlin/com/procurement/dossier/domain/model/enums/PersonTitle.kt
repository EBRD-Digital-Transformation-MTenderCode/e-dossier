package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class PersonTitle(@JsonValue override val key: String) : EnumElementProvider.Key {

    MR("Mr."),
    MS("Ms."),
    MRS("Mrs.");

    override fun toString(): String = key

    companion object : EnumElementProvider<PersonTitle>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = PersonTitle.orThrow(name)
    }
}