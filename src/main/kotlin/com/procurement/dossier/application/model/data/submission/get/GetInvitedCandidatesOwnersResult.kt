package com.procurement.dossier.application.model.data.submission.get

import com.fasterxml.jackson.annotation.JsonProperty
import com.procurement.dossier.domain.model.submission.Submission

data class GetInvitedCandidatesOwnersResult(
    @param:JsonProperty("candidates") @field:JsonProperty("candidates") val candidates: List<Candidate>
) {
    data class Candidate(
        @param:JsonProperty("owner") @field:JsonProperty("owner") val owner: String,
        @param:JsonProperty("organizations") @field:JsonProperty("organizations") val organizations: List<Organization>
    ) { companion object;

        data class Organization(
            @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
            @param:JsonProperty("name") @field:JsonProperty("name") val name: String
        )
    }
}

fun GetInvitedCandidatesOwnersResult.Candidate.Companion.fromDomain(submission: Submission) =
    GetInvitedCandidatesOwnersResult.Candidate(
        owner = submission.owner.toString(),
        organizations = submission.candidates.map { it.convert() }
    )

private fun Submission.Candidate.convert() =
    GetInvitedCandidatesOwnersResult.Candidate.Organization(
        id = id,
        name = name
    )