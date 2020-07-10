package com.procurement.dossier.application.model.data.submission.get


import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.Scale
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.enums.SupplierType
import com.procurement.dossier.domain.model.person.PersonId
import com.procurement.dossier.domain.model.requirement.RequirementId
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueDeserializer
import com.procurement.dossier.infrastructure.bind.criteria.RequirementValueSerializer
import java.time.LocalDateTime

data class GetSubmissionsByQualificationIdsResult(
    @param:JsonProperty("submissions") @field:JsonProperty("submissions") val submissions: Submissions
) {
    data class Submissions(
        @param:JsonProperty("details") @field:JsonProperty("details") val details: List<Detail>
    ) {
        data class Detail(
            @param:JsonProperty("id") @field:JsonProperty("id") val id: SubmissionId,
            @param:JsonProperty("date") @field:JsonProperty("date") val date: LocalDateTime,
            @param:JsonProperty("status") @field:JsonProperty("status") val status: SubmissionStatus,

            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            @param:JsonProperty("requirementResponses") @field:JsonProperty("requirementResponses") val requirementResponses: List<RequirementResponse>?,
            @param:JsonProperty("candidates") @field:JsonProperty("candidates") val candidates: List<Candidate>,
            @param:JsonProperty("documents") @field:JsonProperty("documents") val documents: List<Document>
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
                    @param:JsonProperty("id") @field:JsonProperty("id") val id: RequirementId
                )

                data class RelatedCandidate(
                    @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                    @param:JsonProperty("name") @field:JsonProperty("name") val name: String
                )
            }

            data class Candidate(
                @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                @param:JsonProperty("name") @field:JsonProperty("name") val name: String,
                @param:JsonProperty("identifier") @field:JsonProperty("identifier") val identifier: Identifier,

                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                @param:JsonProperty("additionalIdentifiers") @field:JsonProperty("additionalIdentifiers") val additionalIdentifiers: List<AdditionalIdentifier>?,
                @param:JsonProperty("address") @field:JsonProperty("address") val address: Address,
                @param:JsonProperty("contactPoint") @field:JsonProperty("contactPoint") val contactPoint: ContactPoint,

                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                @param:JsonProperty("persones") @field:JsonProperty("persones") val persones: List<Persone>?,
                @param:JsonProperty("details") @field:JsonProperty("details") val details: Details
            ) {
                data class Identifier(
                    @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                    @param:JsonProperty("legalName") @field:JsonProperty("legalName") val legalName: String,
                    @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,

                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String?
                )

                data class AdditionalIdentifier(
                    @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                    @param:JsonProperty("legalName") @field:JsonProperty("legalName") val legalName: String,
                    @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,

                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String?
                )

                data class Address(
                    @param:JsonProperty("streetAddress") @field:JsonProperty("streetAddress") val streetAddress: String,

                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @param:JsonProperty("postalCode") @field:JsonProperty("postalCode") val postalCode: String?,
                    @param:JsonProperty("addressDetails") @field:JsonProperty("addressDetails") val addressDetails: AddressDetails
                ) {
                    data class AddressDetails(
                        @param:JsonProperty("country") @field:JsonProperty("country") val country: Country,
                        @param:JsonProperty("region") @field:JsonProperty("region") val region: Region,
                        @param:JsonProperty("locality") @field:JsonProperty("locality") val locality: Locality
                    ) {
                        data class Country(
                            @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                            @param:JsonProperty("description") @field:JsonProperty("description") val description: String,
                            @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                            @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String
                        )

                        data class Region(
                            @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                            @param:JsonProperty("description") @field:JsonProperty("description") val description: String,
                            @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                            @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String
                        )

                        data class Locality(
                            @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                            @param:JsonProperty("description") @field:JsonProperty("description") val description: String,
                            @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,

                            @JsonInclude(JsonInclude.Include.NON_NULL)
                            @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String?
                        )
                    }
                }

                data class ContactPoint(
                    @param:JsonProperty("name") @field:JsonProperty("name") val name: String,
                    @param:JsonProperty("email") @field:JsonProperty("email") val email: String,
                    @param:JsonProperty("telephone") @field:JsonProperty("telephone") val telephone: String,

                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @param:JsonProperty("faxNumber") @field:JsonProperty("faxNumber") val faxNumber: String?,

                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @param:JsonProperty("url") @field:JsonProperty("url") val url: String?
                )

                data class Persone(
                    @param:JsonProperty("id") @field:JsonProperty("id") val id: PersonId,
                    @param:JsonProperty("title") @field:JsonProperty("title") val title: String,
                    @param:JsonProperty("name") @field:JsonProperty("name") val name: String,
                    @param:JsonProperty("identifier") @field:JsonProperty("identifier") val identifier: Identifier,
                    @param:JsonProperty("businessFunctions") @field:JsonProperty("businessFunctions") val businessFunctions: List<BusinessFunction>
                ) {
                    data class Identifier(
                        @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,

                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String?
                    )

                    data class BusinessFunction(
                        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                        @param:JsonProperty("type") @field:JsonProperty("type") val type: String,
                        @param:JsonProperty("jobTitle") @field:JsonProperty("jobTitle") val jobTitle: String,
                        @param:JsonProperty("period") @field:JsonProperty("period") val period: Period,

                        @JsonInclude(JsonInclude.Include.NON_EMPTY)
                        @param:JsonProperty("documents") @field:JsonProperty("documents") val documents: List<Document>?
                    ) {
                        data class Period(
                            @param:JsonProperty("startDate") @field:JsonProperty("startDate") val startDate: LocalDateTime
                        )

                        data class Document(
                            @param:JsonProperty("id") @field:JsonProperty("id") val id: DocumentId,
                            @param:JsonProperty("documentType") @field:JsonProperty("documentType") val documentType: DocumentType,
                            @param:JsonProperty("title") @field:JsonProperty("title") val title: String,

                            @JsonInclude(JsonInclude.Include.NON_NULL)
                            @param:JsonProperty("description") @field:JsonProperty("description") val description: String?
                        )
                    }
                }

                data class Details(
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @param:JsonProperty("typeOfSupplier") @field:JsonProperty("typeOfSupplier") val typeOfSupplier: SupplierType?,

                    @JsonInclude(JsonInclude.Include.NON_EMPTY)
                    @param:JsonProperty("mainEconomicActivities") @field:JsonProperty("mainEconomicActivities") val mainEconomicActivities: List<MainEconomicActivity>?,
                    @param:JsonProperty("scale") @field:JsonProperty("scale") val scale: Scale,

                    @JsonInclude(JsonInclude.Include.NON_EMPTY)
                    @param:JsonProperty("bankAccounts") @field:JsonProperty("bankAccounts") val bankAccounts: List<BankAccount>?,

                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @param:JsonProperty("legalForm") @field:JsonProperty("legalForm") val legalForm: LegalForm?
                ) {
                    data class MainEconomicActivity(
                        @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                        @param:JsonProperty("description") @field:JsonProperty("description") val description: String,

                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String?
                    )

                    data class BankAccount(
                        @param:JsonProperty("description") @field:JsonProperty("description") val description: String,
                        @param:JsonProperty("bankName") @field:JsonProperty("bankName") val bankName: String,
                        @param:JsonProperty("address") @field:JsonProperty("address") val address: Address,
                        @param:JsonProperty("identifier") @field:JsonProperty("identifier") val identifier: Identifier,
                        @param:JsonProperty("accountIdentification") @field:JsonProperty("accountIdentification") val accountIdentification: AccountIdentification,

                        @JsonInclude(JsonInclude.Include.NON_EMPTY)
                        @param:JsonProperty("additionalAccountIdentifiers") @field:JsonProperty("additionalAccountIdentifiers") val additionalAccountIdentifiers: List<AdditionalAccountIdentifier>?
                    ) {
                        data class Address(
                            @param:JsonProperty("streetAddress") @field:JsonProperty("streetAddress") val streetAddress: String,

                            @JsonInclude(JsonInclude.Include.NON_NULL)
                            @param:JsonProperty("postalCode") @field:JsonProperty("postalCode") val postalCode: String?,
                            @param:JsonProperty("addressDetails") @field:JsonProperty("addressDetails") val addressDetails: AddressDetails
                        ) {
                            data class AddressDetails(
                                @param:JsonProperty("country") @field:JsonProperty("country") val country: Country,
                                @param:JsonProperty("region") @field:JsonProperty("region") val region: Region,
                                @param:JsonProperty("locality") @field:JsonProperty("locality") val locality: Locality
                            ) {
                                data class Country(
                                    @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                                    @param:JsonProperty("description") @field:JsonProperty("description") val description: String,
                                    @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String
                                )

                                data class Region(
                                    @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                                    @param:JsonProperty("description") @field:JsonProperty("description") val description: String,
                                    @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String
                                )

                                data class Locality(
                                    @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                                    @param:JsonProperty("description") @field:JsonProperty("description") val description: String,
                                    @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String
                                )
                            }
                        }

                        data class Identifier(
                            @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                            @param:JsonProperty("id") @field:JsonProperty("id") val id: String
                        )

                        data class AccountIdentification(
                            @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                            @param:JsonProperty("id") @field:JsonProperty("id") val id: String
                        )

                        data class AdditionalAccountIdentifier(
                            @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                            @param:JsonProperty("id") @field:JsonProperty("id") val id: String
                        )
                    }

                    data class LegalForm(
                        @param:JsonProperty("scheme") @field:JsonProperty("scheme") val scheme: String,
                        @param:JsonProperty("id") @field:JsonProperty("id") val id: String,
                        @param:JsonProperty("description") @field:JsonProperty("description") val description: String,

                        @JsonInclude(JsonInclude.Include.NON_NULL)
                        @param:JsonProperty("uri") @field:JsonProperty("uri") val uri: String?
                    )
                }
            }

            data class Document(
                @param:JsonProperty("documentType") @field:JsonProperty("documentType") val documentType: DocumentType,
                @param:JsonProperty("id") @field:JsonProperty("id") val id: DocumentId,
                @param:JsonProperty("title") @field:JsonProperty("title") val title: String,

                @JsonInclude(JsonInclude.Include.NON_NULL)
                @param:JsonProperty("description") @field:JsonProperty("description") val description: String?
            )
        }
    }
}