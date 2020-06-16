package com.procurement.dossier.infrastructure.model.dto.submission.state.get

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.state.GetSubmissionStateByIdsRequest
import org.junit.jupiter.api.Test

internal class GetSubmissionStateByIdsRequestTest : AbstractDTOTestBase<GetSubmissionStateByIdsRequest>(GetSubmissionStateByIdsRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/state/get/request_get_submission_state_by_ids.json")
    }
}