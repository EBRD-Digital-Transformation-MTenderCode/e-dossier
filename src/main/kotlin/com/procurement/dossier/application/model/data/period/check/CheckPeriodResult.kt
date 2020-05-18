package com.procurement.dossier.application.model.data.period.check

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class CheckPeriodResult(
    @param:JsonProperty("isPreQualificationPeriodChanged") @get:JsonProperty("isPreQualificationPeriodChanged") val isPreQualificationPeriodChanged: Boolean,
    @param:JsonProperty("preQualification") @field:JsonProperty("preQualification") val preQualification: PreQualification
) {
    data class PreQualification(
        @param:JsonProperty("period") @field:JsonProperty("period") val period: Period
    ) {
        data class Period(
            @param:JsonProperty("startDate") @field:JsonProperty("startDate") val startDate: LocalDateTime,
            @param:JsonProperty("endDate") @field:JsonProperty("endDate") val endDate: LocalDateTime
        )
    }
}