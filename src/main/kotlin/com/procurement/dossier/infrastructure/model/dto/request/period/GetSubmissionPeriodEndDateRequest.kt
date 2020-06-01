package com.procurement.dossier.infrastructure.model.dto.request.period


import com.fasterxml.jackson.annotation.JsonProperty

data class GetSubmissionPeriodEndDateRequest(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String
)