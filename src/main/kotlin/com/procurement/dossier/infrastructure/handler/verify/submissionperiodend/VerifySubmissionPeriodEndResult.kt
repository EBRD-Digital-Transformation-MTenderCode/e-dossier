package com.procurement.dossier.infrastructure.handler.verify.submissionperiodend


import com.fasterxml.jackson.annotation.JsonProperty

data class VerifySubmissionPeriodEndResult(
    @get:JsonProperty("submissionPeriodExpired") @field:JsonProperty("submissionPeriodExpired") val submissionPeriodExpired: Boolean
)
