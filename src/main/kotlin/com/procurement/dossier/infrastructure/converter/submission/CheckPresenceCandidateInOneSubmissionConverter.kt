package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.check.CheckPresenceCandidateInOneSubmissionParams
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.infrastructure.model.dto.request.submission.CheckPresenceCandidateInOneSubmissionRequest

fun CheckPresenceCandidateInOneSubmissionRequest.convert() = CheckPresenceCandidateInOneSubmissionParams(
    submissions = CheckPresenceCandidateInOneSubmissionParams.Submissions(
        details = submissions.details.map { detail ->
            CheckPresenceCandidateInOneSubmissionParams.Submissions.Detail(
                id = SubmissionId.create(detail.id),
                candidates = detail.candidates.map { candidate ->
                    CheckPresenceCandidateInOneSubmissionParams.Submissions.Detail.Candidate(candidate.id)
                }
            )
        }
    )
)