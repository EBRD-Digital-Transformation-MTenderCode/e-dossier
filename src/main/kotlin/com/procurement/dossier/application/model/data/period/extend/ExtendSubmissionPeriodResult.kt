package com.procurement.dossier.application.model.data.period.extend

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ExtendSubmissionPeriodResult(
    @param:JsonProperty("startDate") @field:JsonProperty("startDate") val startDate: LocalDateTime,
    @param:JsonProperty("endDate") @field:JsonProperty("endDate") val endDate: LocalDateTime
)
