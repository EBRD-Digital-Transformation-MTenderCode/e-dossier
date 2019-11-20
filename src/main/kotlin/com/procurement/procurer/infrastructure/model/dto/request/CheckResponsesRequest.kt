package com.procurement.procurer.infrastructure.model.dto.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.procurer.application.model.data.RequirementRsValue
import com.procurement.procurer.infrastructure.bind.criteria.RequirementValueDeserializer
import com.procurement.procurer.infrastructure.bind.criteria.RequirementValueSerializer
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeSerializer
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class CheckResponsesRequest(
    @field:JsonProperty("items") @param:JsonProperty("items") val items: List<Item>,
    @field:JsonProperty("bid") @param:JsonProperty("bid") val bid: Bid
) {
    data class Item(
        @field:JsonProperty("id") @param:JsonProperty("id") val id: String
    )

    data class Bid(

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @field:JsonProperty("requirementResponses") @param:JsonProperty("requirementResponses") val requirementResponses: List<RequirementResponse>?,

        @field:JsonProperty("relatedLots") @param:JsonProperty("relatedLots") val relatedLots: List<String>
    ) {
        data class RequirementResponse(
            @field:JsonProperty("id") @param:JsonProperty("id") val id: String,
            @field:JsonProperty("title") @param:JsonProperty("title") val title: String,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            @field:JsonProperty("description") @param:JsonProperty("description") val description: String?,


            @JsonDeserialize(using = RequirementValueDeserializer::class)
            @JsonSerialize(using = RequirementValueSerializer::class)
            @field:JsonProperty("value") @param:JsonProperty("value") val value: RequirementRsValue,

            @field:JsonProperty("requirement") @param:JsonProperty("requirement") val requirement: Requirement,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            @field:JsonProperty("period") @param:JsonProperty("period") val period: Period?
        ) {
            data class Requirement(
                @field:JsonProperty("id") @param:JsonProperty("id") val id: String
            )

            data class Period(
                @JsonDeserialize(using = JsonDateTimeDeserializer::class)
                @JsonSerialize(using = JsonDateTimeSerializer::class)
                @field:JsonProperty("startDate") @param:JsonProperty("startDate") val startDate: LocalDateTime,

                @JsonDeserialize(using = JsonDateTimeDeserializer::class)
                @JsonSerialize(using = JsonDateTimeSerializer::class)
                @field:JsonProperty("endDate") @param:JsonProperty("endDate") val endDate: LocalDateTime
            )
        }
    }
}