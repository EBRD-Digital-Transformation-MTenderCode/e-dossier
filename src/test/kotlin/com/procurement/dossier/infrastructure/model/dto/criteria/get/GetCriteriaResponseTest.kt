package com.procurement.procurer.infrastructure.model.dto.criteria.get

import com.procurement.procurer.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.procurer.infrastructure.model.dto.response.GetCriteriaResponse
import org.junit.jupiter.api.Test

class GetCriteriaResponseTest : AbstractDTOTestBase<GetCriteriaResponse>(
    GetCriteriaResponse::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/service/criteria/get/response_get_criteria_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/service/criteria/get/response_get_criteria_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/service/criteria/get/response_get_criteria_required_2.json")
    }
}
