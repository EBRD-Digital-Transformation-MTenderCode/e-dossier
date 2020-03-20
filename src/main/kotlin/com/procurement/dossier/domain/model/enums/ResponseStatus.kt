package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class ResponseStatus (@JsonValue val value: String){
    SUCCESS("success"),
    ERROR("error"),
    INCIDENT("incident")
}
