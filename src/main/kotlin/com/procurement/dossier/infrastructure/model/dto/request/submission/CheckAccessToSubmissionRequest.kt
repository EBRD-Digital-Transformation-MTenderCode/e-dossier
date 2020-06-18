package com.procurement.dossier.infrastructure.model.dto.request.submission


import com.fasterxml.jackson.annotation.JsonProperty

data class CheckAccessToSubmissionRequest(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String,
    @param:JsonProperty("token") @field:JsonProperty("token") val token: String,
    @param:JsonProperty("owner") @field:JsonProperty("owner") val owner: String,
    @param:JsonProperty("submissionId") @field:JsonProperty("submissionId") val submissionId: String
)