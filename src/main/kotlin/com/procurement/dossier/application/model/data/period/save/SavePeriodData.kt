package com.procurement.dossier.application.model.data.period.save

import java.time.LocalDateTime

data class SavePeriodData(
    val period: Period
) {
    data class Period(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime
    )
}