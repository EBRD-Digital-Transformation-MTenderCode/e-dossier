package com.procurement.dossier.infrastructure.model.dto.period

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.period.ValidatePeriodRequest
import org.junit.jupiter.api.Test

class ValidatePeriodRequestTest : AbstractDTOTestBase<ValidatePeriodRequest>(ValidatePeriodRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/period/validate/request_validate_period.json")
    }
}
