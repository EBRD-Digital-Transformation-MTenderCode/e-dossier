package com.procurement.procurer.infrastructure.model.dto.criteria.ev_panels

import com.procurement.procurer.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.procurer.infrastructure.model.dto.cn.CreateCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.cn.CreateCriteriaResponse
import com.procurement.procurer.infrastructure.model.dto.cn.RequestsForEvPanelsResponse
import org.junit.jupiter.api.Test

class RequestsForEvPanelsResponseTest : AbstractDTOTestBase<RequestsForEvPanelsResponse>(RequestsForEvPanelsResponse::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/service/criteria/ev_panels/response_requests_for_ev_panels_criteria_full.json")
    }

}
