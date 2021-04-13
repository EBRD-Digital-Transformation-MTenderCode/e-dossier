package com.procurement.dossier.application.model.data.submission.check

import com.procurement.dossier.domain.model.submission.SubmissionId

data class CheckPresenceCandidateInOneSubmissionParams(
    val submissions: Submissions
) {
    data class Submissions(
        val details: List<Detail>
    ) {
        data class Detail(
            val id: SubmissionId,
            val candidates: List<Candidate>
        ) {
            data class Candidate(
                val id: String
            )
        }
    }
}