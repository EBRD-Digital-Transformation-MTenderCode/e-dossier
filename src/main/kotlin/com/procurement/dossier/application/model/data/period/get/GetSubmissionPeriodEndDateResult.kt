package com.procurement.dossier.application.model.data.period.get

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class GetSubmissionPeriodEndDateResult(@param:JsonProperty("endDate") @field:JsonProperty("endDate") val endDate: LocalDateTime)
