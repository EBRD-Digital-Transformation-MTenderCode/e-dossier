package com.procurement.dossier.infrastructure.model.dto.submission.check

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.CheckPresenceCandidateInOneSubmissionRequest
import org.junit.jupiter.api.Test

internal class CheckPresenceCandidateInOneSubmissionRequestTest : AbstractDTOTestBase<CheckPresenceCandidateInOneSubmissionRequest>(
    CheckPresenceCandidateInOneSubmissionRequest::class.java
) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/check/request_check_presence_candidate_in_one_submission.json")
    }
}