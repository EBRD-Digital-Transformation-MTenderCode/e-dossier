package com.procurement.dossier.infrastructure.model.dto.request.submission

import com.fasterxml.jackson.annotation.JsonProperty

data class CheckPresenceCandidateInOneSubmissionRequest(
    @param:JsonProperty("submissions") @field:JsonProperty("submissions") val submissions: Submissions
) {
    data class Submissions(
        @param:JsonProperty("details") @field:JsonProperty("details") val details: List<Detail>
    ) {
        data class Detail(
            @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
            @param:JsonProperty("candidates") @field:JsonProperty("candidates") val candidates: List<Candidate>
        ) {
            data class Candidate(
                @param:JsonProperty("id") @field:JsonProperty("id") val id: String
            )
        }
    }
}