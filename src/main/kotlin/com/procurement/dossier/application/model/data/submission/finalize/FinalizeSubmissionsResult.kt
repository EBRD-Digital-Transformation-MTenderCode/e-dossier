package com.procurement.dossier.application.model.data.submission.finalize

import com.fasterxml.jackson.annotation.JsonProperty
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionId

data class FinalizeSubmissionsResult(
    @param:JsonProperty("submissions") @field:JsonProperty("submissions") val submissions: Submissions
) {
    data class Submissions(
        @param:JsonProperty("details") @field:JsonProperty("details") val details: List<Submission>
    ) {
        data class Submission(
            @param:JsonProperty("id") @field:JsonProperty("id") val id: SubmissionId,
            @param:JsonProperty("status") @field:JsonProperty("status") val status: SubmissionStatus
        )
    }
}

fun Submission.convert(): FinalizeSubmissionsResult.Submissions.Submission =
    FinalizeSubmissionsResult.Submissions.Submission(id = this.id, status = this.status)