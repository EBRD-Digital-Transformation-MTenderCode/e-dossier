package com.procurement.dossier.infrastructure.model.dto.period.get

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.period.GetSubmissionPeriodEndDateRequest
import org.junit.jupiter.api.Test

internal class GetSubmissionPeriodEndDateRequestTest : AbstractDTOTestBase<GetSubmissionPeriodEndDateRequest>(GetSubmissionPeriodEndDateRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/period/get/request/request_get_submission_period_end_full.json")
    }
}