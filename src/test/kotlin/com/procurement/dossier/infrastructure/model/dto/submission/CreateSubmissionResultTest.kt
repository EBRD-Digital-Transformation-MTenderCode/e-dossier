package com.procurement.dossier.infrastructure.model.dto.submission

import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class CreateSubmissionResultTest : AbstractDTOTestBase<CreateSubmissionResult>(CreateSubmissionResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/validate/result/result_validate_submission_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/dto/submission/validate/result/result_validate_submission_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/dto/submission/validate/result/result_validate_submission_required_2.json")
    }
}