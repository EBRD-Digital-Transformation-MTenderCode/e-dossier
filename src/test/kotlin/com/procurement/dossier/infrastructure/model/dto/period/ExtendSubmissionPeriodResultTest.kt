package com.procurement.dossier.infrastructure.model.dto.period

import com.procurement.dossier.application.model.data.period.extend.ExtendSubmissionPeriodResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

class ExtendSubmissionPeriodResultTest :
    AbstractDTOTestBase<ExtendSubmissionPeriodResult>(ExtendSubmissionPeriodResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/period/response_extend_submission_period_end.json")
    }
}