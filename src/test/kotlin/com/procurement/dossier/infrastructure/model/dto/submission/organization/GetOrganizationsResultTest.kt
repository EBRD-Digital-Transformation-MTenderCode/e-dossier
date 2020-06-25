package com.procurement.dossier.infrastructure.model.dto.submission.organization

import com.procurement.dossier.application.model.data.submission.organization.GetOrganizationsResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class GetOrganizationsResultTest : AbstractDTOTestBase<GetOrganizationsResult>(GetOrganizationsResult::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/organization/result/result_get_organizations_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/dto/submission/organization/result/result_get_organizations_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/dto/submission/organization/result/result_get_organizations_required_2.json")
    }

    @Test
    fun required_3() {
        testBindingAndMapping("json/dto/submission/organization/result/result_get_organizations_required_3.json")
    }
}