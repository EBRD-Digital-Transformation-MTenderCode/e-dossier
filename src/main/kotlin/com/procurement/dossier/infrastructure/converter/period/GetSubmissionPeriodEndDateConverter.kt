package com.procurement.dossier.infrastructure.converter.period

import com.procurement.dossier.application.model.data.period.get.GetSubmissionPeriodEndDateParams
import com.procurement.dossier.infrastructure.model.dto.request.period.GetSubmissionPeriodEndDateRequest

fun GetSubmissionPeriodEndDateRequest.convert() =
    GetSubmissionPeriodEndDateParams.tryCreate(cpid = cpid, ocid = ocid)