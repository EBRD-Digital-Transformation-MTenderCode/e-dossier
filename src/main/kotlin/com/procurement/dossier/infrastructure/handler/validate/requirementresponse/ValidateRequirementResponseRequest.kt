package com.procurement.dossier.infrastructure.handler.validate.requirementresponse


import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueDeserializer
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueSerializer

data class ValidateRequirementResponseRequest(
    @field:JsonProperty("cpid") @param:JsonProperty("cpid") val cpid: String,
    @field:JsonProperty("requirementResponse") @param:JsonProperty("requirementResponse") val requirementResponse: RequirementResponse
) {
    data class RequirementResponse(
        @field:JsonProperty("id") @param:JsonProperty("id") val id: String,
        @JsonDeserialize(using = RequirementValueDeserializer::class)
        @JsonSerialize(using = RequirementValueSerializer::class)
        @field:JsonProperty("value") @param:JsonProperty("value") val value: RequirementRsValue,
        @field:JsonProperty("requirement") @param:JsonProperty("requirement") val requirement: Requirement
    ) {

        data class Requirement(
            @field:JsonProperty("id") @param:JsonProperty("id") val id: String
        )
    }
}
