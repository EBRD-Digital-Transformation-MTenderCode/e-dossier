package com.procurement.dossier.infrastructure.model.dto.request


import com.fasterxml.jackson.annotation.JsonProperty

data class GetInvitedCandidatesOwnersRequest(
    @param:JsonProperty("cpid") @field:JsonProperty("cpid") val cpid: String,
    @param:JsonProperty("ocid") @field:JsonProperty("ocid") val ocid: String
)