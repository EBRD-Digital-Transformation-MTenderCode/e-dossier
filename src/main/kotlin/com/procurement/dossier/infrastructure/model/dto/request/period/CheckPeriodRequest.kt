package com.procurement.dossier.infrastructure.model.dto.request.period

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CheckPeriodRequest(
    @param:JsonProperty("period") @field:JsonProperty("period") val period: Period
) {
    data class Period(
        @param:JsonProperty("startDate") @field:JsonProperty("startDate") val startDate: LocalDateTime,
        @param:JsonProperty("endDate") @field:JsonProperty("endDate") val endDate: LocalDateTime
    )
}