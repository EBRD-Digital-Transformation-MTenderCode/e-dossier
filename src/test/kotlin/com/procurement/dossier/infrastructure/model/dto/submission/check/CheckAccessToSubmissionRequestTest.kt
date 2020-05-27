package com.procurement.dossier.infrastructure.model.dto.submission.check

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.CheckAccessToSubmissionRequest
import org.junit.jupiter.api.Test

internal class CheckAccessToSubmissionRequestTest : AbstractDTOTestBase<CheckAccessToSubmissionRequest>(CheckAccessToSubmissionRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/check/request_check_access_to_submission.json")
    }
}