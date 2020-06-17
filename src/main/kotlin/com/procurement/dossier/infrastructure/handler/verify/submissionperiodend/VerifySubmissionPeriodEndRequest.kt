package com.procurement.dossier.infrastructure.handler.verify.submissionperiodend


import com.fasterxml.jackson.annotation.JsonProperty

data class VerifySubmissionPeriodEndRequest(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String,
    @param:JsonProperty("date") @field:JsonProperty("date") val date: String
)
