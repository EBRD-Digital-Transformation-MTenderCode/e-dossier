package com.procurement.dossier.application.model.data.period.get.v1

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

class GetPreQualificationPeriodEndResult(
    @param:JsonProperty("preQualification") @field:JsonProperty("preQualification") val preQualification: PreQualification
) {
    data class PreQualification(
        @param:JsonProperty("period") @field:JsonProperty("period") val period: Period
    ) {
        data class Period(
            @param:JsonProperty("endDate") @field:JsonProperty("endDate") val endDate: LocalDateTime
        )
    }
}