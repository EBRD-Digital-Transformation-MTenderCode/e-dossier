package com.procurement.dossier.infrastructure.model.dto.submission.finalize

import com.procurement.dossier.application.model.data.submission.finalize.FinalizeSubmissionsResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class FinalizeSubmissionsResultTest : AbstractDTOTestBase<FinalizeSubmissionsResult>(FinalizeSubmissionsResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/finalize/result_finalize_submission_full.json")
    }
}