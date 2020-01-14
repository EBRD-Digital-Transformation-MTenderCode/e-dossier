package com.procurement.dossier.infrastructure.model.dto.criteria.ev_panels

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.response.RequestsForEvPanelsResponse
import org.junit.jupiter.api.Test

class RequestsForEvPanelsResponseTest : AbstractDTOTestBase<RequestsForEvPanelsResponse>(
    RequestsForEvPanelsResponse::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/service/criteria/ev_panels/response_requests_for_ev_panels_criteria_full.json")
    }

}
