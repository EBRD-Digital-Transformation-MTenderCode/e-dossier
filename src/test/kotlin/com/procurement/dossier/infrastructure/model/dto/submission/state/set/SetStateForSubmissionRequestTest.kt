package com.procurement.dossier.infrastructure.model.dto.submission.state.set

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.state.SetStateForSubmissionRequest
import org.junit.jupiter.api.Test

internal class SetStateForSubmissionRequestTest : AbstractDTOTestBase<SetStateForSubmissionRequest>(SetStateForSubmissionRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/state/set/request_set_state_for_submission.json")
    }
}