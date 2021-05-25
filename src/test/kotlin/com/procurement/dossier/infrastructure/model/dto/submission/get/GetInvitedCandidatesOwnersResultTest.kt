package com.procurement.dossier.infrastructure.model.dto.submission.get

import com.procurement.dossier.application.model.data.submission.get.GetInvitedCandidatesOwnersResult
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

internal class GetInvitedCandidatesOwnersResultTest : AbstractDTOTestBase<GetInvitedCandidatesOwnersResult>(
    GetInvitedCandidatesOwnersResult::class.java
) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/get/result/get_invited_candidates_owners_result_full.json")
    }
}