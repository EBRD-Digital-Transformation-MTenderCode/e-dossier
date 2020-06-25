package com.procurement.dossier.infrastructure.model.dto.submission.organization

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.submission.organization.GetOrganizationsRequest
import org.junit.jupiter.api.Test

internal class GetOrganizationsRequestTest : AbstractDTOTestBase<GetOrganizationsRequest>(GetOrganizationsRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/organization/request/request_get_organizations_full.json")
    }
}