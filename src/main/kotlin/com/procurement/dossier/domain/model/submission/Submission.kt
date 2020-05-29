package com.procurement.dossier.domain.model.submission

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.domain.model.Owner
import com.procurement.dossier.domain.model.Token
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.model.enums.Scale
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.enums.SupplierType
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import java.time.LocalDateTime

data class Submission(
    val id: SubmissionId,
    val date: LocalDateTime,
    val status: SubmissionStatus,
    val token: Token,
    val owner: Owner,
    val candidates: List<Candidate>,
    val requirementResponses: List<RequirementResponse>,
    val documents: List<Document>
) {
    data class Candidate(
        val id: String,
        val name: String,
        val identifier: Identifier,
        val additionalIdentifiers: List<AdditionalIdentifier>,
        val address: Address,
        val contactPoint: ContactPoint,
        val persones: List<Person>,
        val details: Details
    ) {
        data class Identifier(
            val id: String,
            val legalName: String,
            val scheme: String,
            val uri: String?
        )

        data class AdditionalIdentifier(
            val id: String,
            val legalName: String,
            val scheme: String,
            val uri: String?
        )

        data class Address(
            val streetAddress: String,
            val postalCode: String?,
            val addressDetails: AddressDetails
        ) {
            data class AddressDetails(
                val country: Country,
                val region: Region,
                val locality: Locality
            ) {
                data class Country(
                    val id: String,
                    val description: String,
                    val scheme: String,
                    val uri: String
                )

                data class Region(
                    val id: String,
                    val description: String,
                    val scheme: String,
                    val uri: String
                )

                data class Locality(
                    val id: String,
                    val description: String,
                    val scheme: String,
                    val uri: String?
                )
            }
        }

        data class ContactPoint(
            val name: String,
            val email: String,
            val telephone: String,
            val faxNumber: String?,
            val url: String?
        )

        data class Person(
            val id: String,
            val title: PersonTitle,
            val name: String,
            val identifier: Identifier,
            val businessFunctions: List<BusinessFunction>
        ) {
            data class Identifier(
                val scheme: String,
                val id: String, val uri: String?
            )

            data class BusinessFunction(
                val id: String,
                val type: BusinessFunctionType,
                val jobTitle: String,
                val period: Period,
                val documents: List<Document>
            ) {
                data class Period(
                    val startDate: LocalDateTime
                )

                data class Document(
                    val id: DocumentId,
                    val documentType: DocumentType,
                    val title: String,
                    val description: String?
                )
            }
        }

        data class Details(
            val typeOfSupplier: SupplierType?,
            val mainEconomicActivities: List<MainEconomicActivity>,
            val scale: Scale,
            val bankAccounts: List<BankAccount>,
            val legalForm: LegalForm?
        ) {
            data class MainEconomicActivity(
                val scheme: String,
                val id: String,
                val description: String,
                val uri: String?
            )

            data class BankAccount(
                val description: String,
                val bankName: String,
                val address: Address,
                val identifier: Identifier,
                val accountIdentification: AccountIdentification,
                val additionalAccountIdentifiers: List<AdditionalAccountIdentifier>
            ) {
                data class Address(
                    val streetAddress: String,
                    val postalCode: String?,
                    val addressDetails: AddressDetails
                ) {
                    data class AddressDetails(
                        val country: Country,
                        val region: Region,
                        val locality: Locality
                    ) {
                        data class Country(
                            val id: String,
                            val description: String,
                            val scheme: String
                        )

                        data class Region(
                            val id: String,
                            val description: String,
                            val scheme: String
                        )

                        data class Locality(
                            val id: String,
                            val description: String,
                            val scheme: String
                        )
                    }
                }

                data class Identifier(
                    val scheme: String,
                    val id: String
                )

                data class AccountIdentification(
                    val scheme: String,
                    val id: String
                )

                data class AdditionalAccountIdentifier(
                    val scheme: String,
                    val id: String
                )
            }

            data class LegalForm(
                val scheme: String,
                val id: String,
                val description: String,
                val uri: String?
            )
        }
    }

    data class RequirementResponse(
        val id: RequirementResponseId,
        val value: RequirementRsValue,
        val requirement: Requirement,
        val relatedCandidate: RelatedCandidate
    ) {
        data class Requirement(
            val id: String
        )

        data class RelatedCandidate(
            val id: String,
            val name: String
        )
    }

    data class Document(
        val documentType: DocumentType,
        val id: DocumentId,
        val title: String,
        val description: String?
    )
}