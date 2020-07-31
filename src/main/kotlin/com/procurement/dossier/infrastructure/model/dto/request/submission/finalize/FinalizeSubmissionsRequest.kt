package com.procurement.dossier.infrastructure.model.dto.request.submission.finalize

import com.fasterxml.jackson.annotation.JsonProperty

data class FinalizeSubmissionsRequest(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String,
    @param:JsonProperty("qualifications") @field:JsonProperty("qualifications") val qualifications: List<Qualification>
) {
    data class Qualification(
        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
        @param:JsonProperty("status") @field:JsonProperty("status") val status: String,
        @param:JsonProperty("relatedSubmission") @field:JsonProperty("relatedSubmission") val relatedSubmission: String
    )
}