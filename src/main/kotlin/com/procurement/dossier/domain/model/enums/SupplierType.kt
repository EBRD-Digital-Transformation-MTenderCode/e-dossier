package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class SupplierType(@JsonValue override val key: String) : EnumElementProvider.Key {

    COMPANY("company"),
    INDIVIDUAL("individual");

    override fun toString(): String = key

    companion object : EnumElementProvider<SupplierType>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = SupplierType.orThrow(name)
    }
}
