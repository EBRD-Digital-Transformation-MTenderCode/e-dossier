package com.procurement.dossier.infrastructure.model.dto.submission.get

import com.procurement.dossier.application.model.data.submission.get.GetSubmissionsByQualificationIdsResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class GetSubmissionsByQualificationIdsResultTest
    : AbstractDTOTestBase<GetSubmissionsByQualificationIdsResult>(GetSubmissionsByQualificationIdsResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/get/result/get_submissions_by_qualification_ids_result_full.json")
    }

    @Test
    fun required1() {
        testBindingAndMapping("json/dto/submission/get/result/get_submissions_by_qualification_ids_result_required_1.json")
    }

    @Test
    fun required2() {
        testBindingAndMapping("json/dto/submission/get/result/get_submissions_by_qualification_ids_result_required_2.json")
    }

    @Test
    fun required3() {
        testBindingAndMapping("json/dto/submission/get/result/get_submissions_by_qualification_ids_result_required_3.json")
    }
}