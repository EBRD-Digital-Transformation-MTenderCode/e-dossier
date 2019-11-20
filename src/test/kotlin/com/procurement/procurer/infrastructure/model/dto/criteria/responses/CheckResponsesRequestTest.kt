package com.procurement.procurer.infrastructure.model.dto.criteria.responses

import com.procurement.procurer.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.procurer.infrastructure.model.dto.request.CheckResponsesRequest
import org.junit.jupiter.api.Test

class CheckResponsesRequestTest : AbstractDTOTestBase<CheckResponsesRequest>(
    CheckResponsesRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/service/criteria/response/check/request/request_check_responses_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/service/criteria/response/check/request/request_check_responses_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/service/criteria/response/check/request/request_check_responses_required_2.json")
    }

    @Test
    fun required_3() {
        testBindingAndMapping("json/service/criteria/response/check/request/request_check_responses_required_3.json")
    }
}
