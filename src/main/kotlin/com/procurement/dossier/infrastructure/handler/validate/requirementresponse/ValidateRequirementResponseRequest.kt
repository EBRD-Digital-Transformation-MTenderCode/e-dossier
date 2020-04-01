package com.procurement.dossier.infrastructure.handler.validate.requirementresponse


import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueDeserializer
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueSerializer

data class ValidateRequirementResponseRequest(
    @field:JsonProperty("cpid") @param:JsonProperty("cpid") val cpid: String,
    @field:JsonProperty("id") @param:JsonProperty("id") val id: String,
    @field:JsonProperty("ocid") @param:JsonProperty("ocid") val ocid: String,
    @field:JsonProperty("requirementId") @param:JsonProperty("requirementId") val requirementId: String,

    @JsonDeserialize(using = RequirementValueDeserializer::class)
    @JsonSerialize(using = RequirementValueSerializer::class)
    @field:JsonProperty("value") @param:JsonProperty("value") val value: RequirementRsValue
)
