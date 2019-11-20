package com.procurement.procurer.infrastructure.model.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.procurer.application.model.data.CoefficientRate
import com.procurement.procurer.application.model.data.CoefficientValue
import com.procurement.procurer.infrastructure.bind.coefficient.CoefficientRateDeserializer
import com.procurement.procurer.infrastructure.bind.coefficient.CoefficientRateSerializer
import com.procurement.procurer.infrastructure.bind.coefficient.value.CoefficientValueDeserializer
import com.procurement.procurer.infrastructure.bind.coefficient.value.CoefficientValueSerializer
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.model.dto.ocds.ConversionsRelatesTo

data class GetCriteriaResponse(
    @field:JsonProperty("awardCriteria") @param:JsonProperty("awardCriteria") val awardCriteria: AwardCriteria,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @field:JsonProperty("awardCriteriaDetails") @param:JsonProperty("awardCriteriaDetails") val awardCriteriaDetails: AwardCriteriaDetails,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @field:JsonProperty("conversions") @param:JsonProperty("conversions") val conversions: List<Conversion>?
) {
    data class Conversion(
        @field:JsonProperty("id") @param:JsonProperty("id") val id: String,
        @field:JsonProperty("relatesTo") @param:JsonProperty("relatesTo") val relatesTo: ConversionsRelatesTo,
        @field:JsonProperty("relatedItem") @param:JsonProperty("relatedItem") val relatedItem: String,
        @field:JsonProperty("rationale") @param:JsonProperty("rationale") val rationale: String,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @field:JsonProperty("description") @param:JsonProperty("description") val description: String?,

        @field:JsonProperty("coefficients") @param:JsonProperty("coefficients") val coefficients: List<Coefficient>
    ) {
        data class Coefficient(
            @field:JsonProperty("id") @param:JsonProperty("id") val id: String,

            @JsonDeserialize(using = CoefficientValueDeserializer::class)
            @JsonSerialize(using = CoefficientValueSerializer::class)
            @field:JsonProperty("value") @param:JsonProperty("value") val value: CoefficientValue,

            @JsonDeserialize(using = CoefficientRateDeserializer::class)
            @JsonSerialize(using = CoefficientRateSerializer::class)
            @field:JsonProperty("coefficient") @param:JsonProperty("coefficient") val coefficient: CoefficientRate
        )
    }
}