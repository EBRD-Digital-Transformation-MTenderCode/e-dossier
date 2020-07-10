package com.procurement.dossier.infrastructure.model.dto.submission.get

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.GetSubmissionsByQualificationIdsRequest
import org.junit.jupiter.api.Test

internal class GetSubmissionsByQualificationIdsRequestTest : AbstractDTOTestBase<GetSubmissionsByQualificationIdsRequest>(
    GetSubmissionsByQualificationIdsRequest::class.java
) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/get/request/get_submissions_by_qualification_ids_request_full.json")
    }
}