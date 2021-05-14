package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class PartyRole(@JsonValue override val key: String) : EnumElementProvider.Key {

    AUTHOR("author"),
    BUYER("buyer"),
    CANDIDATE("candidate"),
    CENTRAL_PURCHASING_BODY("centralPurchasingBody"),
    CLIENT("client"),
    ENQUIRER("enquirer"),
    FUNDER("funder"),
    INVITED_CANDIDATE("invitedCandidate"),
    INVITED_TENDERER("invitedTenderer"),
    PAYEE("payee"),
    PAYER("payer"),
    PROCURING_ENTITY("procuringEntity"),
    REVIEW_BODY("reviewBody"),
    SUPPLIER("supplier"),
    TENDERER("tenderer");

    override fun toString(): String = key

    companion object : EnumElementProvider<PartyRole>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = orThrow(name)
    }
}
