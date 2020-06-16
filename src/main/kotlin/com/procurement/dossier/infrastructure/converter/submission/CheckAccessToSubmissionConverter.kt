package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.check.CheckAccessToSubmissionParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.CheckAccessToSubmissionRequest

fun CheckAccessToSubmissionRequest.convert() =
    CheckAccessToSubmissionParams.tryCreate(
        cpid = cpid, ocid = ocid, submissionId = submissionId, owner = owner, token = token
    )