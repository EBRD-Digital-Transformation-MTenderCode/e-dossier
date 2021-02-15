package com.procurement.dossier.application.model.data.submission.validate

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.noDuplicatesRule
import com.procurement.dossier.application.model.notEmptyRule
import com.procurement.dossier.application.model.parseBusinessFunctionType
import com.procurement.dossier.application.model.parseDate
import com.procurement.dossier.application.model.parseDocumentId
import com.procurement.dossier.application.model.parseDocumentType
import com.procurement.dossier.application.model.parsePersonTitle
import com.procurement.dossier.application.model.parseScale
import com.procurement.dossier.application.model.parseSupplierType
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.model.enums.Scale
import com.procurement.dossier.domain.model.enums.SupplierType
import com.procurement.dossier.domain.model.evidence.EvidenceId
import com.procurement.dossier.domain.model.requirement.RequirementId
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.validate
import java.time.LocalDateTime

class ValidateSubmissionParams private constructor(
    val id: String,
    val candidates: List<Candidate>,
    val documents: List<Document>,
    val requirementResponses: List<RequirementResponse>,
    val mdm: Mdm
) {
    companion object {
        private const val DOCUMENTS_ATTRIBUTE_NAME = "documents"
        private const val CANDIDATES_ATTRIBUTE_NAME = "candidates"
        private const val REQUIREMENT_RESPONSES_ATTRIBUTE_NAME = "requirementResponses"
        fun tryCreate(
            id: String,
            candidates: List<Candidate>,
            documents: List<Document>?,
            requirementResponses: List<RequirementResponse>?,
            mdm: Mdm
        ): Result<ValidateSubmissionParams, DataErrors> {
            documents.validate(notEmptyRule(DOCUMENTS_ATTRIBUTE_NAME))
                .orForwardFail { return it }
                .validate(noDuplicatesRule(DOCUMENTS_ATTRIBUTE_NAME, { it.id }))
                .orForwardFail { return it }

            requirementResponses.validate(notEmptyRule(REQUIREMENT_RESPONSES_ATTRIBUTE_NAME))
                .orForwardFail { return it }
                .validate(noDuplicatesRule(REQUIREMENT_RESPONSES_ATTRIBUTE_NAME, { it.id }))
                .orForwardFail { return it }

            if (candidates.isEmpty())
                return failure(DataErrors.Validation.EmptyArray(name = CANDIDATES_ATTRIBUTE_NAME))

            return ValidateSubmissionParams(
                id = id,
                candidates = candidates,
                documents = documents ?: emptyList(),
                requirementResponses = requirementResponses ?: emptyList(),
                mdm = mdm
            ).asSuccess()
        }
    }

    class Candidate private constructor(
        val id: String,
        val name: String,
        val identifier: Identifier,
        val additionalIdentifiers: List<AdditionalIdentifier>,
        val address: Address,
        val contactPoint: ContactPoint,
        val persones: List<Person>,
        val details: Details
    ) {
        companion object {
            private const val ADDITIONAL_IDENTIFIER_ATTRIBUTE_NAME = "candidates.additionalIdentifier"
            private const val PERSONES_ATTRIBUTE_NAME = "candidates.persones"
            fun tryCreate(
                id: String,
                name: String,
                identifier: Identifier,
                additionalIdentifiers: List<AdditionalIdentifier>?,
                address: Address,
                contactPoint: ContactPoint,
                persones: List<Person>?,
                details: Details
            ): Result<Candidate, DataErrors> {

                if (additionalIdentifiers != null && additionalIdentifiers.isEmpty())
                    return failure(DataErrors.Validation.EmptyArray(name = ADDITIONAL_IDENTIFIER_ATTRIBUTE_NAME))

                additionalIdentifiers
                    .validate(notEmptyRule(ADDITIONAL_IDENTIFIER_ATTRIBUTE_NAME))
                    .orForwardFail { fail -> return fail }
                    .validate(noDuplicatesRule(ADDITIONAL_IDENTIFIER_ATTRIBUTE_NAME) { it.id })
                    .orForwardFail { fail -> return fail }

                persones
                    .validate(notEmptyRule(PERSONES_ATTRIBUTE_NAME))
                    .orForwardFail { fail -> return fail }
                    .validate(noDuplicatesRule(PERSONES_ATTRIBUTE_NAME) { it.id })
                    .orForwardFail { fail -> return fail }

                return Candidate(
                    id = id,
                    name = name,
                    identifier = identifier,
                    additionalIdentifiers = additionalIdentifiers ?: emptyList(),
                    address = address,
                    contactPoint = contactPoint,
                    persones = persones ?: emptyList(),
                    details = details
                ).asSuccess()
            }
        }

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

        data class ContactPoint(
            val name: String,
            val email: String,
            val telephone: String,
            val faxNumber: String?,
            val url: String?
        )

        class Person private constructor(
            val id: String,
            val title: PersonTitle,
            val name: String,
            val identifier: Identifier,
            val businessFunctions: List<BusinessFunction>
        ) {
            companion object {
                private const val TITLE_ATTRIBUTE_NAME = "candidates.persones.title"
                private const val BUSINESS_FUNCTIONS_ATTRIBUTE_NAME = "candidates.persones.businessFunctions"
                fun tryCreate(
                    id: String,
                    title: String,
                    name: String,
                    identifier: Identifier,
                    businessFunctions: List<BusinessFunction>
                ): Result<Person, DataErrors> {
                    if (businessFunctions.isEmpty())
                        return failure(DataErrors.Validation.EmptyArray(name = BUSINESS_FUNCTIONS_ATTRIBUTE_NAME))

                    val parsedPersonTitle = parsePersonTitle(
                        value = title,
                        attributeName = TITLE_ATTRIBUTE_NAME,
                        allowedEnums = PersonTitle.allowedElements.toSet()
                    ).orForwardFail { fail -> return fail }

                    return Person(
                        id = id,
                        title = parsedPersonTitle,
                        name = name,
                        identifier = identifier,
                        businessFunctions = businessFunctions
                    ).asSuccess()
                }
            }

            data class Identifier(
                val scheme: String,
                val id: String,
                val uri: String?
            )

            class BusinessFunction private constructor(
                val id: String,
                val type: BusinessFunctionType,
                val jobTitle: String,
                val period: Period,
                val documents: List<Document>
            ) {
                companion object {
                    private const val TYPE_ATTRIBUTE_NAME = "candidates.persones.businessFunctions.type"
                    private const val BF_DOCUMENTS_ATTRIBUTE_NAME = "candidates.persones.businessFunctions.documents"

                    private val allowedTypes = BusinessFunctionType.allowedElements
                        .filter { value ->
                            when (value) {
                                BusinessFunctionType.AUTHORITY,
                                BusinessFunctionType.CONTACT_POINT -> true
                                BusinessFunctionType.CHAIRMAN,
                                BusinessFunctionType.PRICE_EVALUATOR,
                                BusinessFunctionType.PRICE_OPENER,
                                BusinessFunctionType.PROCUREMENT_OFFICER,
                                BusinessFunctionType.TECHNICAL_EVALUATOR,
                                BusinessFunctionType.TECHNICAL_OPENER -> false
                            }
                        }.toSet()

                    fun tryCreate(
                        id: String, type: String, jobTitle: String, period: Period, documents: List<Document>?
                    ): Result<BusinessFunction, DataErrors> {
                        if (documents != null && documents.isEmpty())
                            return failure(DataErrors.Validation.EmptyArray(name = BF_DOCUMENTS_ATTRIBUTE_NAME))

                        val parsedType = parseBusinessFunctionType(
                            value = type,
                            attributeName = TYPE_ATTRIBUTE_NAME,
                            allowedEnums = allowedTypes
                        ).orForwardFail { fail -> return fail }

                        return BusinessFunction(
                            id = id,
                            type = parsedType,
                            jobTitle = jobTitle,
                            period = period,
                            documents = documents ?: emptyList()
                        ).asSuccess()
                    }
                }

                class Period private constructor(
                    val startDate: LocalDateTime
                ) {
                    companion object {
                        private const val START_DATE_ATTRIBUTE_NAME = "candidates.persones.businessFunctions.period.startDate"
                        fun tryCreate(startDate: String): Result<Period, DataErrors> {
                            val parsedStartDate = parseDate(
                                value = startDate,
                                attributeName = START_DATE_ATTRIBUTE_NAME
                            )
                                .orForwardFail { fail -> return fail }
                            return Period(startDate = parsedStartDate).asSuccess()
                        }
                    }
                }

                class Document private constructor(
                    val id: DocumentId,
                    val documentType: DocumentType,
                    val title: String,
                    val description: String?
                ) {
                    companion object {
                        private const val DOCUMENT_ID_ATTRIBUTE_NAME = "candidates.persones.businessFunctions.documents.id"
                        private const val DOCUMENT_TYPE_ATTRIBUTE_NAME = "candidates.persones.businessFunctions.documents.documentType"

                        private val allowedDocumentTypes = DocumentType.allowedElements
                            .filter { value ->
                                when (value) {
                                    DocumentType.REGULATORY_DOCUMENT -> true
                                    DocumentType.ILLUSTRATION,
                                    DocumentType.SUBMISSION_DOCUMENTS,
                                    DocumentType.X_ELIGIBILITY_DOCUMENTS,
                                    DocumentType.X_QUALIFICATION_DOCUMENTS,
                                    DocumentType.X_TECHNICAL_DOCUMENTS -> false
                                }
                            }.toSet()

                        fun tryCreate(
                            id: String, documentType: String, title: String, description: String?
                        ): Result<Document, DataErrors> {
                            val parsedId = parseDocumentId(value = id, attributeName = DOCUMENT_ID_ATTRIBUTE_NAME)
                                .orForwardFail { fail -> return fail }

                            val parsedDocumentType = parseDocumentType(
                                value = documentType,
                                attributeName = DOCUMENT_TYPE_ATTRIBUTE_NAME,
                                allowedEnums = allowedDocumentTypes
                            ).orForwardFail { fail -> return fail }

                            return Document(
                                id = parsedId,
                                description = description,
                                documentType = parsedDocumentType,
                                title = title
                            ).asSuccess()
                        }
                    }
                }
            }
        }

        class Details private constructor(
            val typeOfSupplier: SupplierType?,
            val mainEconomicActivities: List<MainEconomicActivity>,
            val scale: Scale,
            val bankAccounts: List<BankAccount>,
            val legalForm: LegalForm?
        ) {
            companion object {
                private const val SUPPLIER_TYPE_ATTRIBUTE_NAME = "candidates.details.typeOfSupplier"
                private const val SCALE_ATTRIBUTE_NAME = "candidates.details.scale"
                private const val MAIN_ECONOMIC_ACTIVITY_ATTRIBUTE_NAME = "candidates.details.mainEconomicActivity"

                private val allowedSupplierTypes = SupplierType.allowedElements
                    .filter { value ->
                        when (value) {
                            SupplierType.COMPANY,
                            SupplierType.INDIVIDUAL -> true
                        }
                    }.toSet()

                private val allowedScales = Scale.allowedElements
                    .filter { value ->
                        when (value) {
                            Scale.LARGE,
                            Scale.MICRO,
                            Scale.SME -> true
                        }
                    }.toSet()

                fun tryCreate(
                    typeOfSupplier: String?,
                    mainEconomicActivities: List<MainEconomicActivity>?,
                    scale: String,
                    bankAccounts: List<BankAccount>?,
                    legalForm: LegalForm?
                ): Result<Details, DataErrors> {

                    mainEconomicActivities
                        .validate(notEmptyRule(MAIN_ECONOMIC_ACTIVITY_ATTRIBUTE_NAME))
                        .orForwardFail { fail -> return fail }
                        .validate(noDuplicatesRule(MAIN_ECONOMIC_ACTIVITY_ATTRIBUTE_NAME) { it.id })
                        .orForwardFail { fail -> return fail }

                    val parsedTypeOfSupplier = typeOfSupplier?.let {
                        parseSupplierType(
                            value = it,
                            attributeName = SUPPLIER_TYPE_ATTRIBUTE_NAME,
                            allowedEnums = allowedSupplierTypes
                        ).orForwardFail { fail -> return fail }
                    }

                    val parsedScale = parseScale(
                        value = scale,
                        attributeName = SCALE_ATTRIBUTE_NAME,
                        allowedEnums = allowedScales
                    ).orForwardFail { fail -> return fail }

                    return Details(
                        typeOfSupplier = parsedTypeOfSupplier,
                        mainEconomicActivities = mainEconomicActivities ?: emptyList(),
                        scale = parsedScale,
                        bankAccounts = bankAccounts ?: emptyList(),
                        legalForm = legalForm
                    ).asSuccess()
                }
            }

            data class MainEconomicActivity(
                val scheme: String,
                val id: String,
                val description: String,
                val uri: String?
            )

            class BankAccount private constructor(
                val description: String,
                val bankName: String,
                val address: Address,
                val identifier: Identifier,
                val accountIdentification: AccountIdentification,
                val additionalAccountIdentifiers: List<AdditionalAccountIdentifier>
            ) {
                companion object {
                    private const val ADDITIONAL_ACC_IDENTIFIERS_ATTRIBUTE_NAME = "candidates.details.bankAccount.additionalAccountIdentifiers"

                    fun tryCreate(
                        description: String,
                        bankName: String,
                        address: Address,
                        identifier: Identifier,
                        accountIdentification: AccountIdentification,
                        additionalAccountIdentifiers: List<AdditionalAccountIdentifier>?
                    ): Result<BankAccount, DataErrors> {
                        if (additionalAccountIdentifiers != null && additionalAccountIdentifiers.isEmpty())
                            return failure(DataErrors.Validation.EmptyArray(name = ADDITIONAL_ACC_IDENTIFIERS_ATTRIBUTE_NAME))

                        return BankAccount(
                            description = description,
                            additionalAccountIdentifiers = additionalAccountIdentifiers ?: emptyList(),
                            accountIdentification = accountIdentification,
                            address = address,
                            identifier = identifier,
                            bankName = bankName
                        ).asSuccess()
                    }
                }

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

    class Document private constructor(
        val documentType: DocumentType,
        val id: DocumentId,
        val title: String,
        val description: String?
    ) {
        companion object {
            private const val DOCUMENT_ID_ATTRIBUTE_NAME = "documents.id"
            private const val DOCUMENT_TYPE_ATTRIBUTE_NAME = "documents.documentType"

            private val allowedDocumentTypes = DocumentType.allowedElements
                .filter { value ->
                    when (value) {
                        DocumentType.ILLUSTRATION,
                        DocumentType.SUBMISSION_DOCUMENTS,
                        DocumentType.X_ELIGIBILITY_DOCUMENTS,
                        DocumentType.X_QUALIFICATION_DOCUMENTS,
                        DocumentType.X_TECHNICAL_DOCUMENTS -> true
                        DocumentType.REGULATORY_DOCUMENT -> false

                    }
                }.toSet()

            fun tryCreate(
                id: String, documentType: String, title: String, description: String?
            ): Result<Document, DataErrors> {
                val parsedId = parseDocumentId(value = id, attributeName = DOCUMENT_ID_ATTRIBUTE_NAME)
                    .orForwardFail { fail -> return fail }

                val parsedDocumentType = parseDocumentType(
                    value = documentType,
                    attributeName = DOCUMENT_TYPE_ATTRIBUTE_NAME,
                    allowedEnums = allowedDocumentTypes
                ).orForwardFail { fail -> return fail }

                return Document(
                    id = parsedId,
                    description = description,
                    documentType = parsedDocumentType,
                    title = title
                ).asSuccess()
            }
        }
    }

    class RequirementResponse private constructor(
        val id: RequirementResponseId,
        val value: RequirementRsValue,
        val requirement: Requirement,
        val relatedCandidate: RelatedCandidate,
        val evidences: List<Evidence>
    ) {
        companion object {
            private const val EVIDENCE_ATTRIBUTE_NAME = "requirementResponses.evidences"

            fun tryCreate(
                id: String,
                value: RequirementRsValue,
                requirement: Requirement,
                relatedCandidate: RelatedCandidate,
                evidences: List<Evidence>?
            ): Result<RequirementResponse, DataErrors> {
                evidences.validate(notEmptyRule(EVIDENCE_ATTRIBUTE_NAME))
                    .orForwardFail { return it }
                    .validate(noDuplicatesRule(EVIDENCE_ATTRIBUTE_NAME, { it.id }))
                    .orForwardFail { return it }

                return RequirementResponse(
                    id = id,
                    requirement = requirement,
                    value = value,
                    evidences = evidences ?: emptyList(),
                    relatedCandidate = relatedCandidate
                ).asSuccess()
            }
        }

        data class Requirement(
            val id: RequirementId
        )

        data class RelatedCandidate(
            val name: String,
            val id: String
        )

        data class Evidence(
            val id: EvidenceId,
            val title: String,
            val description: String?,
            val relatedDocument: RelatedDocument?
        ) {
            data class RelatedDocument(
                val id: DocumentId
            )
        }
    }

    class Mdm private constructor(
        val registrationSchemes: List<RegistrationScheme>
    ) {
        companion object{
            private const val REGISTRATION_SCHEMES_ATTRIBUTE_NAME = "mdm.registrationSchemes"


            fun tryCreate(registrationSchemes: List<RegistrationScheme>): Result<Mdm, DataErrors>{
                registrationSchemes.validate(notEmptyRule(REGISTRATION_SCHEMES_ATTRIBUTE_NAME))
                    .orForwardFail { return it }

                return Mdm(registrationSchemes).asSuccess()
            }
        }

        class RegistrationScheme private constructor(
            val country: String,
            val schemes: List<String>
        ) {
            companion object {
                private const val SCHEMES_ATTRIBUTE_NAME = "mdm.registrationSchemes.schemes"


                fun tryCreate(country: String, schemes: List<String>): Result<RegistrationScheme, DataErrors> {
                    schemes.validate(notEmptyRule(SCHEMES_ATTRIBUTE_NAME))
                        .orForwardFail { return it }

                    return RegistrationScheme(country, schemes).asSuccess()
                }
            }
        }
    }
}