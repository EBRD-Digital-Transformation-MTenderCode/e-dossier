package com.procurement.dossier.infrastructure.model.dto.period

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.period.SavePeriodRequest
import org.junit.jupiter.api.Test

class SavePeriodRequestTest : AbstractDTOTestBase<SavePeriodRequest>(SavePeriodRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/period/save/request_save_period.json")
    }
}
