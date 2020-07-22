package com.procurement.dossier.infrastructure.model.dto.submission.find

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.FindSubmissionsRequest
import org.junit.jupiter.api.Test

internal class FindSubmissionsRequestTest : AbstractDTOTestBase<FindSubmissionsRequest>(
    FindSubmissionsRequest::class.java
) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/find/request/request_find_submission_for_opening_full.json")
    }
}