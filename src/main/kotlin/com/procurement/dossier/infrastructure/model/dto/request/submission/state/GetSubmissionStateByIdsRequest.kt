package com.procurement.dossier.infrastructure.model.dto.request.submission.state


import com.fasterxml.jackson.annotation.JsonProperty

data class GetSubmissionStateByIdsRequest(
    @param:JsonProperty("submissionIds") @field:JsonProperty("submissionIds") val submissionIds: List<String>,
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String
)