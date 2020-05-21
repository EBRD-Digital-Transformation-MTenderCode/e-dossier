package com.procurement.dossier.infrastructure.model.dto.submission

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.ValidateSubmissionRequest
import org.junit.jupiter.api.Test

internal class ValidateSubmissionRequestTest : AbstractDTOTestBase<ValidateSubmissionRequest>(ValidateSubmissionRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/validate/request/request_validate_submission_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/dto/submission/validate/request/request_validate_submission_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/dto/submission/validate/request/request_validate_submission_required_2.json")
    }

    @Test
    fun required_3() {
        testBindingAndMapping("json/dto/submission/validate/request/request_validate_submission_required_3.json")
    }
}