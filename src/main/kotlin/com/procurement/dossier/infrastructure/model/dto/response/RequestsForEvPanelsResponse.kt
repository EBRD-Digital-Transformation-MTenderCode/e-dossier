package com.procurement.dossier.infrastructure.model.dto.response


import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.dossier.application.model.data.Requirement
import com.procurement.dossier.infrastructure.bind.criteria.RequirementDeserializer
import com.procurement.dossier.infrastructure.bind.criteria.RequirementSerializer
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource
import java.util.*

data class RequestsForEvPanelsResponse(
    @field:JsonProperty("criteria") @param:JsonProperty("criteria") val criteria: Criteria
) {
    data class Criteria(
        @field:JsonProperty("id") @param:JsonProperty("id") val id: UUID,
        @field:JsonProperty("title") @param:JsonProperty("title") val title: String,
        @field:JsonProperty("source") @param:JsonProperty("source") val source: CriteriaSource,
        @field:JsonProperty("relatesTo") @param:JsonProperty("relatesTo") val relatesTo: CriteriaRelatesTo,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @field:JsonProperty("description") @param:JsonProperty("description") val description: String?,

        @field:JsonProperty("requirementGroups") @param:JsonProperty("requirementGroups") val requirementGroups: List<RequirementGroup>
    ) {
        data class RequirementGroup(
            @field:JsonProperty("id") @param:JsonProperty("id") val id: UUID,

            @JsonDeserialize(using = RequirementDeserializer::class)
            @JsonSerialize(using = RequirementSerializer::class)
            @field:JsonProperty("requirements") @param:JsonProperty("requirements") val requirements: List<Requirement>
        )
    }
}