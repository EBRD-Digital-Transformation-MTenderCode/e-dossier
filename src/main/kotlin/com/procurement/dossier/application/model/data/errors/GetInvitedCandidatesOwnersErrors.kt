package com.procurement.dossier.application.model.data.errors

import com.procurement.dossier.domain.fail.error.ValidationCommandError
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid

sealed class GetInvitedCandidatesOwnersErrors(
    override val description: String,
    numberError: String
) : ValidationCommandError() {

    override val code: String = prefix + numberError

    class SubmissionNotFound(cpid: Cpid, ocid: Ocid) : GetInvitedCandidatesOwnersErrors(
        numberError = "5.21.1",
        description = "Submission not found by cpid '$cpid' and ocid '$ocid'."
    )
}

