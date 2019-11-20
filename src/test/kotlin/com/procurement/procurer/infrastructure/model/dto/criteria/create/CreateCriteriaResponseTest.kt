package com.procurement.procurer.infrastructure.model.dto.criteria.create

import com.procurement.procurer.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.procurer.infrastructure.model.dto.response.CreateCriteriaResponse
import org.junit.jupiter.api.Test

class CreateCriteriaResponseTest : AbstractDTOTestBase<CreateCriteriaResponse>(
    CreateCriteriaResponse::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/service/criteria/create/response/response_create_criteria_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/service/criteria/create/response/response_create_criteria_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/service/criteria/create/response/response_create_criteria_required_2.json")
    }

}
