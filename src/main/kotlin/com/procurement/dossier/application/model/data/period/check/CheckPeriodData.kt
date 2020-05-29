package com.procurement.dossier.application.model.data.period.check

import java.time.LocalDateTime

data class CheckPeriodData(
    val period: Period
) {
    data class Period(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime
    )
}