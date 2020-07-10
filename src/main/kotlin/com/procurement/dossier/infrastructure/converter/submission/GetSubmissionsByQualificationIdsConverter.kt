package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.get.GetSubmissionsByQualificationIdsParams
import com.procurement.dossier.domain.util.extension.mapResult
import com.procurement.dossier.infrastructure.model.dto.request.submission.GetSubmissionsByQualificationIdsRequest

fun GetSubmissionsByQualificationIdsRequest.convert() = GetSubmissionsByQualificationIdsParams.tryCreate(
    cpid = cpid,
    ocid = ocid,
    qualifications = qualifications.mapResult { it.convert() }.orForwardFail { fail -> return fail }
)

fun GetSubmissionsByQualificationIdsRequest.Qualification.convert() =
    GetSubmissionsByQualificationIdsParams.Qualification.tryCreate(id = id, relatedSubmission = relatedSubmission)
