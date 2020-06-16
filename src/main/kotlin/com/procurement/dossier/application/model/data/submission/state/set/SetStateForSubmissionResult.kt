package com.procurement.dossier.application.model.data.submission.state.set

import com.fasterxml.jackson.annotation.JsonProperty
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.submission.SubmissionId

data class SetStateForSubmissionResult(
    @param:JsonProperty("id") @field:JsonProperty("id") val id: SubmissionId,
    @param:JsonProperty("status") @field:JsonProperty("status") val status: SubmissionStatus
)