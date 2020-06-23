package com.procurement.dossier.infrastructure.model.dto.request.submission


import com.fasterxml.jackson.annotation.JsonProperty

data class FindSubmissionsForOpeningRequest(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String,
    @param:JsonProperty("pmd") @field:JsonProperty("pmd") val pmd: String,
    @param:JsonProperty("country") @field:JsonProperty("country") val country: String
)