package com.procurement.dossier.application.model.data.submission.create

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.parseBusinessFunctionType
import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseDate
import com.procurement.dossier.application.model.parseDocumentId
import com.procurement.dossier.application.model.parseDocumentType
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parseOwner
import com.procurement.dossier.application.model.parsePersonTitle
import com.procurement.dossier.application.model.parseRequirementResponseId
import com.procurement.dossier.application.model.parseScale
import com.procurement.dossier.application.model.parseSupplierType
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.Owner
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.model.enums.Scale
import com.procurement.dossier.domain.model.enums.SupplierType
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.asSuccess
import java.time.LocalDateTime

class CreateSubmissionParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid,
    val date: LocalDateTime,
    val owner: Owner,
    val submission: Submission
) {
    companion object {
        private const val DATE_ATTRIBUTE_NAME = "date"

        fun tryCreate(
            cpid: String,
            ocid: String,
            date: String,
            owner: String,
            submission: Submission
        ): Result<CreateSubmissionParams, DataErrors> {
            val cpidParsed = parseCpid(cpid)
                .doReturn { error -> return failure(error = error) }

            val ocidParsed = parseOcid(ocid)
                .doReturn { error -> return failure(error = error) }

            val ownerParsed = parseOwner(owner)
                .doReturn { error -> return failure(error = error) }

            val dateParsed = parseDate(date, DATE_ATTRIBUTE_NAME)
                .doReturn { error -> return failure(error = error) }

            return CreateSubmissionParams(
                cpid = cpidParsed,
                ocid = ocidParsed,
                date = dateParsed,
                owner = ownerParsed,
                submission = submission
            ).asSuccess()
        }
    }

    class Submission private constructor(
        val id: SubmissionId,
        val candidates: List<Candidate>,
        val documents: List<Document>,
        val requirementResponses: List<RequirementResponse>
    ) {
        companion object {
            private const val DOCUMENTS_ATTRIBUTE_NAME = "submission.documents"
            private const val CANDIDATES_ATTRIBUTE_NAME = "submission.candidates"
            private const val REQUIREMENT_RS_ATTRIBUTE_NAME = "submission.requirementResponses"
            private const val ID_ATTRIBUTE_NAME = "submission.id"

            fun tryCreate(
                id: String,
                candidates: List<Candidate>,
                documents: List<Document>?,
                requirementResponses: List<RequirementResponse>?
            ): Result<Submission, DataErrors> {
                if (documents != null && documents.isEmpty())
                    return failure(DataErrors.Validation.EmptyArray(name = DOCUMENTS_ATTRIBUTE_NAME))

                if (candidates.isEmpty())
                    return failure(DataErrors.Validation.EmptyArray(name = CANDIDATES_ATTRIBUTE_NAME))

                if (requirementResponses != null && requirementResponses.isEmpty())
                    return failure(DataErrors.Validation.EmptyArray(name = REQUIREMENT_RS_ATTRIBUTE_NAME))

                val idParsed = SubmissionId.create(id)

                return Submission(
                    id = idParsed,
                    candidates = candidates,
                    documents = documents ?: emptyList(),
                    requirementResponses = requirementResponses ?: emptyList()
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
                private const val ADDITIONAL_IDENTIFIER_ATTRIBUTE_NAME = "submission.candidates.additionalIdentifiers"
                private const val PERSONES_ATTRIBUTE_NAME = "submission.candidates.persones"
                fun tryCreate(
                    id: String,
                    name: String,
                    identifier: Identifier,
                    additionalIdentifier: List<AdditionalIdentifier>?,
                    address: Address,
                    contactPoint: ContactPoint,
                    persones: List<Person>?,
                    details: Details
                ): Result<Candidate, DataErrors> {

                    if (additionalIdentifier != null && additionalIdentifier.isEmpty())
                        return failure(DataErrors.Validation.EmptyArray(name = ADDITIONAL_IDENTIFIER_ATTRIBUTE_NAME))

                    if (persones != null && persones.isEmpty())
                        return failure(DataErrors.Validation.EmptyArray(name = PERSONES_ATTRIBUTE_NAME))

                    return Candidate(
                        id = id,
                        name = name,
                        identifier = identifier,
                        additionalIdentifiers = additionalIdentifier ?: emptyList(),
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

            class Person private constructor(
                val id: String,
                val title: PersonTitle,
                val name: String,
                val identifier: Identifier,
                val businessFunctions: List<BusinessFunction>
            ) {
                companion object {
                    private const val TITLE_ATTRIBUTE_NAME = "submission.candidates.persones.title"
                    private const val BUSINESS_FUNCTIONS_ATTRIBUTE_NAME = "submission.candidates.persones.businessFunctions"
                    fun tryCreate(
                        id: String, title: String, name: String, identifier: Identifier, businessFunctions: List<BusinessFunction>
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
                        private const val TYPE_ATTRIBUTE_NAME = "submission.candidates.persones.businessFunctions.type"
                        private const val BF_DOCUMENTS_ATTRIBUTE_NAME = "submission.candidates.persones.businessFunctions.documents"

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
                            private const val START_DATE_ATTRIBUTE_NAME = "submission.candidates.persones.businessFunctions.period.startDate"
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
                            private const val DOCUMENT_ID_ATTRIBUTE_NAME = "submission.candidates.persones.businessFunctions.documents.id"
                            private const val DOCUMENT_TYPE_ATTRIBUTE_NAME = "submission.candidates.persones.businessFunctions.documents.documentType"

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
                    private const val SUPPLIER_TYPE_ATTRIBUTE_NAME = "submission.candidates.details.typeOfSupplier"
                    private const val SCALE_ATTRIBUTE_NAME = "submission.candidates.details.scale"
                    private const val MAIN_ECONOMIC_ACTIVITY_ATTRIBUTE_NAME = "candidates.details.mainEconomicActivities"

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
                        mainEconomicActivity: List<MainEconomicActivity>?,
                        scale: String,
                        bankAccounts: List<BankAccount>?,
                        legalForm: LegalForm?
                    ): Result<Details, DataErrors> {
                        if (mainEconomicActivity != null && mainEconomicActivity.isEmpty())
                            return failure(DataErrors.Validation.EmptyArray(name = MAIN_ECONOMIC_ACTIVITY_ATTRIBUTE_NAME))

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
                            mainEconomicActivities = mainEconomicActivity ?: emptyList(),
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
                        private const val ADDITIONAL_ACC_IDENTIFIERS_ATTRIBUTE_NAME = "submission.candidates.details.bankAccount.additionalAccountIdentifiers"

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
                private const val DOCUMENT_ID_ATTRIBUTE_NAME = "submission.documents.id"
                private const val DOCUMENT_TYPE_ATTRIBUTE_NAME = "submission.documents.documentType"

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
            val relatedCandidate: RelatedCandidate
        ) {
            companion object {
                fun tryCreate(
                    id: String,
                    value: RequirementRsValue,
                    requirement: Requirement,
                    relatedCandidate: RelatedCandidate
                ): Result<RequirementResponse, DataErrors> {
                    val parsedRequirementResponseId = parseRequirementResponseId(id)
                        .orForwardFail { fail -> return fail }
                    return RequirementResponse(
                        id = parsedRequirementResponseId,
                        relatedCandidate = relatedCandidate,
                        requirement = requirement,
                        value = value
                    ).asSuccess()
                }
            }

            data class Requirement(
                val id: String
            )

            data class RelatedCandidate(
                val id: String,
                val name: String
            )
        }
    }
}