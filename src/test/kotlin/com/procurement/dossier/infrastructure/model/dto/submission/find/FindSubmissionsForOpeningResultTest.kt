package com.procurement.dossier.infrastructure.model.dto.submission.find

import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsForOpeningResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.CreateSubmissionRequest
import com.procurement.dossier.infrastructure.model.dto.request.submission.FindSubmissionsForOpeningRequest
import org.junit.jupiter.api.Test

internal class FindSubmissionsForOpeningResultTest : AbstractDTOTestBase<FindSubmissionsForOpeningResult>(FindSubmissionsForOpeningResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/find/result/result_find_submission_for_opening_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/dto/submission/find/result/result_find_submission_for_opening_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/dto/submission/find/result/result_find_submission_for_opening_required_2.json")
    }

    @Test
    fun required_3() {
        testBindingAndMapping("json/dto/submission/find/result/result_find_submission_for_opening_required_3.json")
    }
}