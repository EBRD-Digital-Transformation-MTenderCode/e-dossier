package com.procurement.dossier.infrastructure.model.dto.submission.get

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.GetInvitedCandidatesOwnersRequest
import org.junit.jupiter.api.Test

internal class GetInvitedCandidatesOwnersRequestTest : AbstractDTOTestBase<GetInvitedCandidatesOwnersRequest>(
    GetInvitedCandidatesOwnersRequest::class.java
) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/get/request/get_invited_candidates_owners_request_full.json")
    }
}