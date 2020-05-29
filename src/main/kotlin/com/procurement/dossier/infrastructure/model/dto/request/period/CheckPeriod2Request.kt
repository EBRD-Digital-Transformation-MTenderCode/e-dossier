package com.procurement.dossier.infrastructure.model.dto.request.period


import com.fasterxml.jackson.annotation.JsonProperty

data class CheckPeriod2Request(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String,
    @param:JsonProperty("date") @field:JsonProperty("date") val date: String
)