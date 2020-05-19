package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class BusinessFunctionType(@JsonValue override val key: String) : EnumElementProvider.Key {

    AUTHORITY("authority"),
    CHAIRMAN("chairman"),
    PROCUREMENT_OFFICER("procurementOfficer"),
    CONTACT_POINT("contactPoint"),
    TECHNICAL_EVALUATOR("technicalEvaluator"),
    TECHNICAL_OPENER("technicalOpener"),
    PRICE_OPENER("priceOpener"),
    PRICE_EVALUATOR("priceEvaluator");

    override fun toString(): String = key

    companion object : EnumElementProvider<BusinessFunctionType>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = BusinessFunctionType.orThrow(name)
    }
}