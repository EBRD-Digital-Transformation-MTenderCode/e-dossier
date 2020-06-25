package com.procurement.dossier.infrastructure.model.dto.period.get

import com.procurement.dossier.application.model.data.period.get.GetSubmissionPeriodEndDateResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class GetSubmissionPeriodEndDateResultTest : AbstractDTOTestBase<GetSubmissionPeriodEndDateResult>(GetSubmissionPeriodEndDateResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/period/get/result/result_get_submission_period_end_full.json")
    }
}