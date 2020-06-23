package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.organization.GetOrganizationsParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.organization.GetOrganizationsRequest

fun GetOrganizationsRequest.convert() =
    GetOrganizationsParams.tryCreate(cpid = cpid, ocid = ocid)