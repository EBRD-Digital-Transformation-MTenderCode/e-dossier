package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.FindSubmissionsRequest

fun FindSubmissionsRequest.convert() = FindSubmissionsParams.tryCreate(
    cpid = cpid,
    ocid = ocid,
    country = country,
    pmd = pmd,
    operationType = operationType
)