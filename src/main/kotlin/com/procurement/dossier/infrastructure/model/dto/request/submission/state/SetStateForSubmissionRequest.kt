package com.procurement.dossier.infrastructure.model.dto.request.submission.state


import com.fasterxml.jackson.annotation.JsonProperty

data class SetStateForSubmissionRequest(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String,
    @param:JsonProperty("submission") @field:JsonProperty("submission") val submission: Submission
) {
    data class Submission(
        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
        @param:JsonProperty("status") @field:JsonProperty("status") val status: String
    )
}