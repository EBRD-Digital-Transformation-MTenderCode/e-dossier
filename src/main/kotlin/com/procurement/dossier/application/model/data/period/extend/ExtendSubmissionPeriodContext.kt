package com.procurement.dossier.application.model.data.period.extend

import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod

class ExtendSubmissionPeriodContext(val cpid: Cpid, val ocid: Ocid, val country: String, val pmd: ProcurementMethod)