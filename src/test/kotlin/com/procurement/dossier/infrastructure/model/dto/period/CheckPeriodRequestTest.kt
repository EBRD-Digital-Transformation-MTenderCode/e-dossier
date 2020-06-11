package com.procurement.dossier.infrastructure.model.dto.period

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.period.CheckPeriodRequest
import org.junit.jupiter.api.Test

class CheckPeriodRequestTest : AbstractDTOTestBase<CheckPeriodRequest>(CheckPeriodRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/period/check/request_check_period.json")
    }
}
