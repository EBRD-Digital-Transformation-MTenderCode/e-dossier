package com.procurement.dossier.infrastructure.model.dto.submission.state.set

import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class SetStateForSubmissionResultTest : AbstractDTOTestBase<SetStateForSubmissionResult>(SetStateForSubmissionResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/state/set/result_set_state_for_submission.json")
    }
}