package com.procurement.dossier.infrastructure.model.dto.submission.state.get

import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class GetSubmissionStateByIdsResultTest : AbstractDTOTestBase<GetSubmissionStateByIdsResult>(GetSubmissionStateByIdsResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/state/get/result_get_submission_state_by_ids.json")
    }
}