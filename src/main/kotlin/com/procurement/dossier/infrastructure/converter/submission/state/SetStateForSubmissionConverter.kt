package com.procurement.dossier.infrastructure.converter.submission.state

import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.state.SetStateForSubmissionRequest

fun SetStateForSubmissionRequest.convert() =
    SetStateForSubmissionParams.tryCreate(
        cpid = cpid,
        ocid = ocid,
        submission = submission.convert().orForwardFail { fail -> return fail })

private fun SetStateForSubmissionRequest.Submission.convert() =
    SetStateForSubmissionParams.Submission.tryCreate(id = id, status = status)