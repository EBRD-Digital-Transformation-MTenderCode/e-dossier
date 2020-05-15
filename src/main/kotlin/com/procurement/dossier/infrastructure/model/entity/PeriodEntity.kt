package com.procurement.dossier.infrastructure.model.entity

import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import java.time.LocalDateTime

data class PeriodEntity(
    val cpid: Cpid,
    val ocid: Ocid,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)