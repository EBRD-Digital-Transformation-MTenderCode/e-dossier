package com.procurement.dossier.infrastructure.model.dto.submission.finalize

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.finalize.FinalizeSubmissionsRequest
import org.junit.jupiter.api.Test

internal class FinalizeSubmissionsRequestTest : AbstractDTOTestBase<FinalizeSubmissionsRequest>(FinalizeSubmissionsRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/finalize/request_finalize_submission_full.json")
    }
}