package com.procurement.dossier.infrastructure.converter.submission.state

import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.state.GetSubmissionStateByIdsRequest

fun GetSubmissionStateByIdsRequest.convert() =
    GetSubmissionStateByIdsParams.tryCreate(cpid = cpid, ocid = ocid, submissionIds = submissionIds)