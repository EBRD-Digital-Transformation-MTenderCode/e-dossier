package com.procurement.dossier.application.model.data.submission.create

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.domain.model.Token
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueDeserializer
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueSerializer
import java.time.LocalDateTime

data class CreateSubmissionResult(
    @param:JsonProperty("id") @field:JsonProperty("id") val id: SubmissionId,
    @param:JsonProperty("date") @field:JsonProperty("date") val date: LocalDateTime,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @param:JsonProperty("requirementResponses") @field:JsonProperty("requirementResponses") val requirementResponses: List<RequirementResponse>?,
    @param:JsonProperty("candidates") @field:JsonProperty("candidates") val candidates: List<Candidate>,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @param:JsonProperty("documents") @field:JsonProperty("documents") val documents: List<Document>?,
    @param:JsonProperty("status") @field:JsonProperty("status") val status: SubmissionStatus,
    @param:JsonProperty("token") @field:JsonProperty("token") val token: Token
) {
    data class RequirementResponse(
        @param:JsonProperty("id") @field:JsonProperty("id") val id: RequirementResponseId,

        @JsonDeserialize(using = RequirementValueDeserializer::class)
        @JsonSerialize(using = RequirementValueSerializer::class)
        @param:JsonProperty("value") @field:JsonProperty("value") val value: RequirementRsValue,
        @param:JsonProperty("requirement") @field:JsonProperty("requirement") val requirement: Requirement,
        @param:JsonProperty("relatedCandidate") @field:JsonProperty("relatedCandidate") val relatedCandidate: RelatedCandidate
    ) {
        data class Requirement(
            @param:JsonProperty("id") @field:JsonProperty("id") val id: String
        )

        data class RelatedCandidate(
            @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
            @param:JsonProperty("name") @field:JsonProperty("name") val name: String
        )
    }

    data class Candidate(
        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
        @param:JsonProperty("name") @field:JsonProperty("name") val name: String
    )

    data class Document(
        @param:JsonProperty("documentType") @field:JsonProperty("documentType") val documentType: DocumentType,
        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
        @param:JsonProperty("title") @field:JsonProperty("title") val title: String,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @param:JsonProperty("description") @field:JsonProperty("description") val description: String?
    )
}