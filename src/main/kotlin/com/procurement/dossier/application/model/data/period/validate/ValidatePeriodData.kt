package com.procurement.dossier.application.model.data.period.validate

import java.time.LocalDateTime

data class ValidatePeriodData(
    val period: Period
) {
    data class Period(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime
    )
}