package com.procurement.procurer.infrastructure.model.dto.criteria.check

import com.procurement.procurer.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.procurer.infrastructure.model.dto.request.CheckCriteriaRequest
import org.junit.jupiter.api.Test

class CheckCriteriaRequestTest : AbstractDTOTestBase<CheckCriteriaRequest>(
    CheckCriteriaRequest::class.java) {
    @Test
    fun fully() {
        testBindingAndMapping("json/service/criteria/check/request/request_check_criteria_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/service/criteria/check/request/request_check_criteria_required_1.json")
    }

}
