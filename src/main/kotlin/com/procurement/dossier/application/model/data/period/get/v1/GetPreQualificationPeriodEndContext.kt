package com.procurement.dossier.application.model.data.period.get.v1

import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid

class GetPreQualificationPeriodEndContext(
    val cpid: Cpid,
    val ocid: Ocid
)