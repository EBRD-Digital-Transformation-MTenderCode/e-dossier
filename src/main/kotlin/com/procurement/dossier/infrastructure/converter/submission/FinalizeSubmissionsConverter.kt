package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.finalize.FinalizeSubmissionsParams
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.extension.mapResult
import com.procurement.dossier.infrastructure.model.dto.request.submission.finalize.FinalizeSubmissionsRequest

fun FinalizeSubmissionsRequest.convert(): Result<FinalizeSubmissionsParams, DataErrors.Validation> {

    val convertedQualifications = this.qualifications
        .mapResult { it.convert() }
        .orForwardFail { fail -> return fail }

    return FinalizeSubmissionsParams.tryCreate(
        cpid = this.cpid,
        ocid = this.ocid,
        qualifications = convertedQualifications
    )
}

private fun FinalizeSubmissionsRequest.Qualification.convert()
    : Result<FinalizeSubmissionsParams.Qualification, DataErrors.Validation> =
    FinalizeSubmissionsParams.Qualification.tryCreate(
        id = this.id, status = this.status, relatedSubmission = this.relatedSubmission
    )
