package com.procurement.dossier.infrastructure.handler.verify.submissionperiodend

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

class VerifySubmissionPeriodEndRequestTest : AbstractDTOTestBase<VerifySubmissionPeriodEndRequest>(
    VerifySubmissionPeriodEndRequest::class.java
) {
    @Test
    fun test() {
        testBindingAndMapping("json/handler/verify/submissionperiodend/verify_submission_period_end_request_full.json")
    }
}
