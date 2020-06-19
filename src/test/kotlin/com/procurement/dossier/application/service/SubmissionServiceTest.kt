package com.procurement.dossier.application.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.data.submission.check.CheckAccessToSubmissionParams
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsParams
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsResult
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionParams
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionResult
import com.procurement.dossier.application.model.data.submission.validate.ValidateSubmissionParams
import com.procurement.dossier.application.repository.SubmissionRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.model.enums.Scale
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.enums.SupplierType
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionCredentials
import com.procurement.dossier.domain.model.submission.SubmissionState
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.format
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SubmissionServiceTest {
    companion object {
        private val CPID = Cpid.tryCreateOrNull("ocds-t1s2t3-MD-1565251033096")!!
        private val OCID = Ocid.tryCreateOrNull("ocds-b3wdp1-MD-1581509539187-EV-1581509653044")!!
        private val OWNER = UUID.randomUUID()
        private val TOKEN = UUID.randomUUID()
        private val SUBMISSION_ID_1 = UUID.randomUUID()
        private val SUBMISSION_ID_2 = UUID.randomUUID()

        private val submissionRepository: SubmissionRepository = mock()
        private val generable: Generable = mock()
        private val submissionService: SubmissionService = SubmissionService(submissionRepository, generable)
    }

    @Nested
    inner class GetSubmissionStateByIds {

        @Test
        fun success() {
            val params = getParams()
            val storedStates = listOf(
                SubmissionState(id = params.submissionIds[0], status = SubmissionStatus.PENDING),
                SubmissionState(id = params.submissionIds[1], status = SubmissionStatus.PENDING)
            )
            whenever(
                submissionRepository.getSubmissionsStates(
                    cpid = params.cpid, ocid = params.ocid, submissionIds = params.submissionIds
                )
            )
                .thenReturn(storedStates.asSuccess())

            val actual = submissionService.getSubmissionStateByIds(params = params).get
            val expected = listOf(
                GetSubmissionStateByIdsResult(id = params.submissionIds[0], status = SubmissionStatus.PENDING),
                GetSubmissionStateByIdsResult(id = params.submissionIds[1], status = SubmissionStatus.PENDING)
            )

            assertEquals(actual, expected)
        }

        @Test
        fun notAllSubmissionsFound_fail() {
            val params = getParams()
            whenever(
                submissionRepository.getSubmissionsStates(
                    cpid = params.cpid, ocid = params.ocid, submissionIds = params.submissionIds
                )
            )
                .thenReturn(
                    listOf(SubmissionState(id = params.submissionIds[0], status = SubmissionStatus.PENDING)).asSuccess()
                )

            val actual = submissionService.getSubmissionStateByIds(params = params).error

            assertTrue(actual is ValidationErrors.SubmissionNotFoundFor)
        }

        @Test
        fun databaseIncident_fail() {
            val params = getParams()
            val expected = Result.failure(
                Fail.Incident.Database.Interaction(
                    exception = RuntimeException("")
                )
            )
            whenever(
                submissionRepository.getSubmissionsStates(
                    cpid = params.cpid, ocid = params.ocid, submissionIds = params.submissionIds
                )
            )
                .thenReturn(expected)

            val actual = submissionService.getSubmissionStateByIds(params = params)

            assertEquals(actual, expected)
        }

        private fun getParams() =
            GetSubmissionStateByIdsParams.tryCreate(
                cpid = CPID.toString(),
                ocid = OCID.toString(),
                submissionIds = listOf(
                    SUBMISSION_ID_1.toString(), SUBMISSION_ID_2.toString()
                )
            ).get
    }

    @Nested
    inner class SetStateForSubmission {
        @Test
        fun success() {
            val params = getParams()
            val requestSubmission = params.submission
            val storedSubmission = stubSubmission().copy(id = requestSubmission.id, status = SubmissionStatus.WITHDRAWN)

            whenever(
                submissionRepository.findSubmission(
                    cpid = params.cpid,
                    ocid = params.ocid,
                    id = requestSubmission.id
                )
            ).thenReturn(storedSubmission.asSuccess())

            whenever(
                submissionRepository.updateSubmission(
                    cpid = eq(params.cpid),
                    ocid = eq(params.ocid),
                    submission = any()
                )
            ).thenReturn(true.asSuccess())

            val actual = submissionService.setStateForSubmission(params = params).get
            val expected = SetStateForSubmissionResult(id = requestSubmission.id, status = requestSubmission.status)

            assertEquals(actual, expected)
        }

        @Test
        fun submissionNotFound_fail() {
            val params = getParams()
            val requestSubmission = params.submission
            whenever(
                submissionRepository.findSubmission(
                    cpid = params.cpid,
                    ocid = params.ocid,
                    id = requestSubmission.id
                )
            ).thenReturn(null.asSuccess())

            val actual = submissionService.setStateForSubmission(params = params).error

            assertTrue(actual is ValidationErrors.SubmissionNotFoundFor.SetStateForSubmission)
        }

        @Test
        fun databaseIncident_fail() {
            val params = getParams()
            val requestSubmission = params.submission
            val expected = Result.failure(
                Fail.Incident.Database.Interaction(
                    exception = RuntimeException("")
                )
            )
            whenever(
                submissionRepository.findSubmission(
                    cpid = params.cpid,
                    ocid = params.ocid,
                    id = requestSubmission.id
                )
            ).thenReturn(expected)

            val actual = submissionService.setStateForSubmission(params = params)

            assertEquals(expected, actual)
        }

        private fun getParams(): SetStateForSubmissionParams =
            SetStateForSubmissionParams.tryCreate(
                cpid = CPID.toString(),
                ocid = OCID.toString(),
                submission = SetStateForSubmissionParams.Submission.tryCreate(
                    id = SUBMISSION_ID_1.toString(),
                    status = SubmissionStatus.PENDING.key
                ).get
            ).get
    }

    @Nested
    inner class CheckAccessToSubmission {
        @Test
        fun success() {
            val params = getParams()
            val credentials = SubmissionCredentials(
                id = params.submissionId,
                token = params.token,
                owner = params.owner
            )
            whenever(
                submissionRepository.getSubmissionCredentials(
                    cpid = params.cpid, ocid = params.ocid, id = params.submissionId
                )
            ).thenReturn(credentials.asSuccess())

            val actual = submissionService.checkAccessToSubmission(params)

            assertTrue(actual is ValidationResult.Ok)
        }

        @Test
        fun submissionNotFound_fail() {
            val params = getParams()
            whenever(
                submissionRepository.getSubmissionCredentials(
                    cpid = params.cpid, ocid = params.ocid, id = params.submissionId
                )
            ).thenReturn(null.asSuccess())

            val actual = submissionService.checkAccessToSubmission(params).error

            assertTrue(actual is ValidationErrors.SubmissionNotFoundFor.CheckAccessToSubmission)
        }

        @Test
        fun tokenNotMatch_fail() {
            val params = getParams()
            val credentials = SubmissionCredentials(
                id = params.submissionId,
                token = UUID.randomUUID(),
                owner = params.owner
            )
            whenever(
                submissionRepository.getSubmissionCredentials(
                    cpid = params.cpid, ocid = params.ocid, id = params.submissionId
                )
            ).thenReturn(credentials.asSuccess())

            val actual = submissionService.checkAccessToSubmission(params).error

            assertTrue(actual is ValidationErrors.InvalidToken)
        }

        @Test
        fun ownerNotMatch_fail() {
            val params = getParams()
            val credentials = SubmissionCredentials(
                id = params.submissionId,
                token = params.token,
                owner = UUID.randomUUID()
            )
            whenever(
                submissionRepository.getSubmissionCredentials(
                    cpid = params.cpid, ocid = params.ocid, id = params.submissionId
                )
            ).thenReturn(credentials.asSuccess())

            val actual = submissionService.checkAccessToSubmission(params).error

            assertTrue(actual is ValidationErrors.InvalidOwner)
        }

        private fun getParams() = CheckAccessToSubmissionParams
            .tryCreate(
                submissionId = SUBMISSION_ID_1.toString(),
                cpid = CPID.toString(),
                ocid = OCID.toString(),
                owner = OWNER.toString(),
                token = TOKEN.toString()
            ).get
    }

    @Nested
    inner class ValidateSubmission {

        @Test
        fun success() {

            val businessFunctionDocumentFirst = createBusinessFunctionDocument("bf_document_1")
            val businessFunctionDocumentSecond = createBusinessFunctionDocument("bf_document_2")
            val businessFunctionDocumentThird = createBusinessFunctionDocument("bf_document_3")
            val businessFunctionDocumentFourth = createBusinessFunctionDocument("bf_document_4")
            val businessFunctionDocumentFifth = createBusinessFunctionDocument("bf_document_5")

            val businessFunctionFirst = createBusinessFunction(
                id = "bf1", documents = listOf(businessFunctionDocumentFirst, businessFunctionDocumentSecond)
            )
            val businessFunctionSecond = createBusinessFunction(
                id = "bf2", documents = listOf(businessFunctionDocumentThird, businessFunctionDocumentFourth)
            )
            val businessFunctionThird = createBusinessFunction(
                id = "bf3", documents = listOf(businessFunctionDocumentFifth)
            )

            val personFirst = createPerson(listOf(businessFunctionFirst, businessFunctionSecond))
            val personSecond = createPerson(listOf(businessFunctionThird))

            val candidateFirst = createCandidate(id = UUID.randomUUID(), persones = listOf(personFirst, personSecond))
            val candidateSecond = createCandidate(id = UUID.randomUUID(), persones = listOf(personFirst))

            val documentFirst = createDocument(id = "document_1")
            val documentSecond = createDocument(id = "document_2")

            val params = createValidateSubmissionParams(
                candidates = listOf(candidateFirst, candidateSecond),
                documents = listOf(documentFirst, documentSecond)
            )

            val actual = submissionService.validateSubmission(params)

            assertTrue(actual is ValidationResult.Ok)
        }

        @Test
        fun duplicateBusinessFunctionInDifferentPersones_success() {

            val businessFunctionDocumentFirst = createBusinessFunctionDocument("bf_document_1")
            val businessFunctionDocumentSecond = createBusinessFunctionDocument("bf_document_2")
            val businessFunctionDocumentThird = createBusinessFunctionDocument("bf_document_3")
            val businessFunctionDocumentFourth = createBusinessFunctionDocument("bf_document_4")

            val businessFunctionFirst = createBusinessFunction(
                id = "bf1", documents = listOf(businessFunctionDocumentFirst, businessFunctionDocumentSecond)
            )
            val businessFunctionSecond = createBusinessFunction(
                id = "bf2", documents = listOf(businessFunctionDocumentThird, businessFunctionDocumentFourth)
            )

            val personFirst = createPerson(listOf(businessFunctionFirst, businessFunctionSecond))
            val personSecond = createPerson(listOf(businessFunctionFirst, businessFunctionSecond))

            val candidate = createCandidate(id = UUID.randomUUID(), persones = listOf(personFirst, personSecond))

            val documentFirst = createDocument(id = "document_1")
            val documentSecond = createDocument(id = "document_2")

            val params = createValidateSubmissionParams(
                candidates = listOf(candidate),
                documents = listOf(documentFirst, documentSecond)
            )

            val actual = submissionService.validateSubmission(params)

            assertTrue(actual is ValidationResult.Ok)
        }

        @Test
        fun duplicateCandidate_fail() {

            val businessFunctionDocumentFirst = createBusinessFunctionDocument("bf_document_1")
            val businessFunctionDocumentSecond = createBusinessFunctionDocument("bf_document_2")
            val businessFunctionDocumentThird = createBusinessFunctionDocument("bf_document_3")
            val businessFunctionDocumentFourth = createBusinessFunctionDocument("bf_document_4")
            val businessFunctionDocumentFifth = createBusinessFunctionDocument("bf_document_5")

            val businessFunctionFirst = createBusinessFunction(
                id = "bf1", documents = listOf(businessFunctionDocumentFirst, businessFunctionDocumentSecond)
            )
            val businessFunctionSecond = createBusinessFunction(
                id = "bf2", documents = listOf(businessFunctionDocumentThird, businessFunctionDocumentFourth)
            )
            val businessFunctionThird = createBusinessFunction(
                id = "bf3", documents = listOf(businessFunctionDocumentFifth)
            )

            val personFirst = createPerson(listOf(businessFunctionFirst, businessFunctionSecond))
            val personSecond = createPerson(listOf(businessFunctionThird))

            val candidateId = UUID.randomUUID()
            val candidate = createCandidate(id = candidateId, persones = listOf(personFirst, personSecond))

            val documentFirst = createDocument(id = "document_1")
            val documentSecond = createDocument(id = "document_2")

            val params = createValidateSubmissionParams(
                candidates = listOf(candidate, candidate),
                documents = listOf(documentFirst, documentSecond)
            )

            val actual = submissionService.validateSubmission(params).error

            assertTrue(actual is ValidationErrors.Duplicate.Candidate)
            assertEquals("Value '$candidateId' is not unique in 'candidates.id'.", actual.description)
        }

        @Test
        fun duplicateBusinessFunctionsWithinOnePerson_fail() {

            val businessFunctionDocumentFirst = createBusinessFunctionDocument("bf_document_1")
            val businessFunctionDocumentSecond = createBusinessFunctionDocument("bf_document_2")

            val businessFunction = createBusinessFunction(
                id = "bf1", documents = listOf(businessFunctionDocumentFirst, businessFunctionDocumentSecond)
            )

            val person = createPerson(listOf(businessFunction, businessFunction))
            val candidate = createCandidate(id = UUID.randomUUID(), persones = listOf(person))

            val documentFirst = createDocument(id = "document_1")
            val documentSecond = createDocument(id = "document_2")

            val params = createValidateSubmissionParams(
                candidates = listOf(candidate),
                documents = listOf(documentFirst, documentSecond)
            )

            val actual = submissionService.validateSubmission(params).error

            assertTrue(actual is ValidationErrors.Duplicate.PersonBusinessFunction)
            assertEquals(
                "Value '${businessFunction.id}' is not unique in 'candidates.persons.businessFunctions.id'.",
                actual.description
            )
        }

        @Test
        fun duplicateDocumentsWithinOnePerson_fail() {

            val businessFunctionDocumentFirst = createBusinessFunctionDocument("bf_document_1")
            val businessFunctionDocumentSecond = createBusinessFunctionDocument("bf_document_2")
            val businessFunctionDocumentThird = createBusinessFunctionDocument("bf_document_3")

            val businessFunctionFirst = createBusinessFunction(
                id = "bf1", documents = listOf(businessFunctionDocumentFirst, businessFunctionDocumentSecond)
            )
            val businessFunctionSecond = createBusinessFunction(
                id = "bf2", documents = listOf(businessFunctionDocumentFirst, businessFunctionDocumentThird)
            )

            val person = createPerson(listOf(businessFunctionFirst, businessFunctionSecond))

            val candidate = createCandidate(id = UUID.randomUUID(), persones = listOf(person))

            val documentFirst = createDocument(id = "document_1")
            val documentSecond = createDocument(id = "document_2")

            val params = createValidateSubmissionParams(
                candidates = listOf(candidate),
                documents = listOf(documentFirst, documentSecond)
            )

            val actual = submissionService.validateSubmission(params).error

            assertTrue(actual is ValidationErrors.Duplicate.PersonDocument)
            assertEquals(
                "Value '${businessFunctionDocumentFirst.id}' is not unique in 'candidates.persons.businessFunctions.documents.id'.",
                actual.description
            )
        }

        @Test
        fun duplicateDocuments_fail() {

            val businessFunctionDocumentFirst = createBusinessFunctionDocument("bf_document_1")
            val businessFunctionDocumentSecond = createBusinessFunctionDocument("bf_document_2")
            val businessFunctionDocumentThird = createBusinessFunctionDocument("bf_document_3")
            val businessFunctionDocumentFourth = createBusinessFunctionDocument("bf_document_4")
            val businessFunctionDocumentFifth = createBusinessFunctionDocument("bf_document_5")

            val businessFunctionFirst = createBusinessFunction(
                id = "bf1", documents = listOf(businessFunctionDocumentFirst, businessFunctionDocumentSecond)
            )
            val businessFunctionSecond = createBusinessFunction(
                id = "bf2", documents = listOf(businessFunctionDocumentThird, businessFunctionDocumentFourth)
            )
            val businessFunctionThird = createBusinessFunction(
                id = "bf3", documents = listOf(businessFunctionDocumentFifth)
            )

            val personFirst = createPerson(listOf(businessFunctionFirst, businessFunctionSecond))
            val personSecond = createPerson(listOf(businessFunctionThird))

            val candidateFirst = createCandidate(id = UUID.randomUUID(), persones = listOf(personFirst, personSecond))
            val candidateSecond = createCandidate(id = UUID.randomUUID(), persones = listOf(personFirst))

            val document = createDocument(id = "document_1")

            val params = createValidateSubmissionParams(
                candidates = listOf(candidateFirst, candidateSecond),
                documents = listOf(document, document)
            )

            val actual = submissionService.validateSubmission(params).error

            assertTrue(actual is ValidationErrors.Duplicate.OrganizationDocument)
            assertEquals("Value '${document.id}' is not unique in 'documents.id'.", actual.description)
        }

        private fun createValidateSubmissionParams(
            documents: List<ValidateSubmissionParams.Document>,
            candidates: List<ValidateSubmissionParams.Candidate>
        ) = ValidateSubmissionParams
            .tryCreate(
                id = UUID.randomUUID().toString(),
                documents = documents,
                candidates = candidates
            ).get

        private fun createCandidate(
            id: UUID,
            persones: List<ValidateSubmissionParams.Candidate.Person>
        ) = ValidateSubmissionParams.Candidate.tryCreate(
            id = id.toString(),
            name = "candidate.name",
            identifier = ValidateSubmissionParams.Candidate.Identifier(
                id = "identifier.id",
                scheme = "identifier.scheme",
                uri = "identifier.uri",
                legalName = "identifier.legalName"
            ),
            additionalIdentifiers = listOf(
                ValidateSubmissionParams.Candidate.AdditionalIdentifier(
                    id = "additionalIdentifier.id",
                    scheme = "additionalIdentifier.scheme",
                    uri = "additionalIdentifier.uri",
                    legalName = "additionalIdentifier.legalName"
                )
            ),
            persones = persones,
            address = ValidateSubmissionParams.Candidate.Address(
                streetAddress = "streetAddress",
                postalCode = "postalCode",
                addressDetails = ValidateSubmissionParams.Candidate.Address.AddressDetails(
                    country = ValidateSubmissionParams.Candidate.Address.AddressDetails.Country(
                        id = "country.id",
                        scheme = "country.scheme",
                        description = "country.description"
                    ),
                    region = ValidateSubmissionParams.Candidate.Address.AddressDetails.Region(
                        id = "region.id",
                        scheme = "region.scheme",
                        description = "region.description"
                    ),
                    locality = ValidateSubmissionParams.Candidate.Address.AddressDetails.Locality(
                        id = "locality.id",
                        scheme = "locality.scheme",
                        description = "locality.description"
                    )
                )
            ),
            contactPoint = ValidateSubmissionParams.Candidate.ContactPoint(
                name = "contactPoint.name",
                url = "contactPoint.url",
                telephone = "contactPoint.telephone",
                faxNumber = "contactPoint.faxNumber",
                email = "contactPoint.email"
            ),
            details = ValidateSubmissionParams.Candidate.Details.tryCreate(
                typeOfSupplier = SupplierType.COMPANY.key,
                scale = Scale.LARGE.key,
                mainEconomicActivities = listOf(
                    ValidateSubmissionParams.Candidate.Details.MainEconomicActivity(
                        id = "mainEconomicActivities.id",
                        scheme = "mainEconomicActivities.scheme",
                        description = "mainEconomicActivities.description",
                        uri = "mainEconomicActivities.uri"
                    )
                ),
                legalForm = ValidateSubmissionParams.Candidate.Details.LegalForm(
                    id = "legalForm.id",
                    scheme = "legalForm.scheme",
                    description = "legalForm.description",
                    uri = "legalForm.uri"
                ),
                bankAccounts = listOf(
                    ValidateSubmissionParams.Candidate.Details.BankAccount.tryCreate(
                        description = "legalForm.bankAccounts",
                        identifier = ValidateSubmissionParams.Candidate.Details.BankAccount.Identifier(
                            id = "bankAccounts.identifier.id",
                            scheme = "bankAccounts.identifier.scheme"
                        ),
                        bankName = "bankName",
                        additionalAccountIdentifiers = listOf(
                            ValidateSubmissionParams.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                id = "bankAccounts.additionalAccountIdentifiers.id",
                                scheme = "bankAccounts.additionalAccountIdentifiers.scheme"
                            )
                        ),
                        accountIdentification = ValidateSubmissionParams.Candidate.Details.BankAccount.AccountIdentification(
                            id = "bankAccounts.accountIdentification.id",
                            scheme = "bankAccounts.accountIdentification.scheme"
                        ),
                        address = ValidateSubmissionParams.Candidate.Details.BankAccount.Address(
                            streetAddress = "streetAddress",
                            postalCode = "postalCode",
                            addressDetails = ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails(
                                country = ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                    id = "country.id",
                                    scheme = "country.scheme",
                                    description = "country.description"
                                ),
                                region = ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails.Region(
                                    id = "region.id",
                                    scheme = "region.scheme",
                                    description = "region.description"
                                ),
                                locality = ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                    id = "locality.id",
                                    scheme = "locality.scheme",
                                    description = "locality.description"
                                )
                            )
                        )
                    ).get
                )
            ).get
        ).get

        private fun createPerson(businessFunctions: List<ValidateSubmissionParams.Candidate.Person.BusinessFunction>) = ValidateSubmissionParams.Candidate.Person.tryCreate(
            id = "person.id",
            title = PersonTitle.MR.key,
            identifier = ValidateSubmissionParams.Candidate.Person.Identifier(
                id = "persones.identifier.id",
                scheme = "persones.identifier.scheme",
                uri = "persones.identifier.uri"
            ),
            name = "persones.name",
            businessFunctions = businessFunctions
        ).get

        private fun createBusinessFunction(
            id: String,
            documents: List<ValidateSubmissionParams.Candidate.Person.BusinessFunction.Document>
        ) = ValidateSubmissionParams.Candidate.Person.BusinessFunction.tryCreate(
            id = id,
            type = BusinessFunctionType.CONTACT_POINT.key,
            documents = documents,
            jobTitle = "jobTitle",
            period = ValidateSubmissionParams.Candidate.Person.BusinessFunction.Period.tryCreate(
                startDate = LocalDateTime.now().format()
            ).get
        ).get

        private fun createBusinessFunctionDocument(id: DocumentId) = ValidateSubmissionParams.Candidate.Person.BusinessFunction.Document.tryCreate(
            documentType = DocumentType.REGULATORY_DOCUMENT.key,
            id = id,
            description = "businessFunctions.document.description",
            title = "businessFunctions.document.title"
        ).get

        private fun createDocument(id: DocumentId) = ValidateSubmissionParams.Document.tryCreate(
            documentType = DocumentType.X_TECHNICAL_DOCUMENTS.key,
            id = id,
            description = "document.description",
            title = "document.title"
        ).get
    }

    private fun stubSubmission() =
        Submission(
            id = UUID.randomUUID(),
            status = SubmissionStatus.PENDING,
            owner = UUID.randomUUID(),
            token = UUID.randomUUID(),
            date = JsonDateTimeDeserializer.deserialize(JsonDateTimeSerializer.serialize(LocalDateTime.now())),
            documents = listOf(
                Submission.Document(
                    documentType = DocumentType.REGULATORY_DOCUMENT,
                    id = "document.id",
                    description = "document.description",
                    title = "document.title"
                )
            ),
            requirementResponses = listOf(
                Submission.RequirementResponse(
                    id = "requirementResponse.id",
                    value = RequirementRsValue.AsString("requirementResponse.value"),
                    requirement = Submission.RequirementResponse.Requirement(id = "requirementResponse.requirement.id"),
                    relatedCandidate = Submission.RequirementResponse.RelatedCandidate(
                        id = "relatedCandidate.id",
                        name = "relatedCandidate.name"
                    )
                )
            ),
            candidates = listOf(
                Submission.Candidate(
                    id = "candidate.id",
                    name = "candidate.name",
                    identifier = Submission.Candidate.Identifier(
                        id = "identifier.id",
                        scheme = "identifier.scheme",
                        uri = "identifier.uri",
                        legalName = "identifier.legalName"
                    ),
                    additionalIdentifiers = listOf(
                        Submission.Candidate.AdditionalIdentifier(
                            id = "additionalIdentifier.id",
                            scheme = "additionalIdentifier.scheme",
                            uri = "additionalIdentifier.uri",
                            legalName = "additionalIdentifier.legalName"
                        )
                    ),
                    persones = listOf(
                        Submission.Candidate.Person(
                            id = "person.id",
                            title = PersonTitle.MR,
                            identifier = Submission.Candidate.Person.Identifier(
                                id = "persones.identifier.id",
                                scheme = "persones.identifier.scheme",
                                uri = "persones.identifier.uri"
                            ),
                            name = "persones.name",
                            businessFunctions = listOf(
                                Submission.Candidate.Person.BusinessFunction(
                                    id = "businessFunction.id",
                                    type = BusinessFunctionType.CHAIRMAN,
                                    documents = listOf(
                                        Submission.Candidate.Person.BusinessFunction.Document(
                                            documentType = DocumentType.REGULATORY_DOCUMENT,
                                            id = "businessFunctions.document.id",
                                            description = "businessFunctions.document.description",
                                            title = "businessFunctions.document.title"
                                        )
                                    ),
                                    jobTitle = "jobTitle",
                                    period = Submission.Candidate.Person.BusinessFunction.Period(
                                        startDate = JsonDateTimeDeserializer.deserialize(
                                            JsonDateTimeSerializer.serialize(
                                                LocalDateTime.now()
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    address = Submission.Candidate.Address(
                        streetAddress = "streetAddress",
                        postalCode = "postalCode",
                        addressDetails = Submission.Candidate.Address.AddressDetails(
                            country = Submission.Candidate.Address.AddressDetails.Country(
                                id = "country.id",
                                scheme = "country.scheme",
                                description = "country.description",
                                uri = "country.uri"
                            ),
                            region = Submission.Candidate.Address.AddressDetails.Region(
                                id = "region.id",
                                scheme = "region.scheme",
                                description = "region.description",
                                uri = "region.uri"
                            ),
                            locality = Submission.Candidate.Address.AddressDetails.Locality(
                                id = "locality.id",
                                scheme = "locality.scheme",
                                description = "locality.description",
                                uri = "locality.uri"
                            )
                        )
                    ),
                    contactPoint = Submission.Candidate.ContactPoint(
                        name = "contactPoint.name",
                        url = "contactPoint.url",
                        telephone = "contactPoint.telephone",
                        faxNumber = "contactPoint.faxNumber",
                        email = "contactPoint.email"
                    ),
                    details = Submission.Candidate.Details(
                        typeOfSupplier = SupplierType.COMPANY,
                        scale = Scale.LARGE,
                        mainEconomicActivities = listOf(
                            Submission.Candidate.Details.MainEconomicActivity(
                                id = "mainEconomicActivities.id",
                                scheme = "mainEconomicActivities.scheme",
                                description = "mainEconomicActivities.description",
                                uri = "mainEconomicActivities.uri"
                            )
                        ),
                        legalForm = Submission.Candidate.Details.LegalForm(
                            id = "legalForm.id",
                            scheme = "legalForm.scheme",
                            description = "legalForm.description",
                            uri = "legalForm.uri"
                        ),
                        bankAccounts = listOf(
                            Submission.Candidate.Details.BankAccount(
                                description = "legalForm.bankAccounts",
                                identifier = Submission.Candidate.Details.BankAccount.Identifier(
                                    id = "bankAccounts.identifier.id",
                                    scheme = "bankAccounts.identifier.scheme"
                                ),
                                bankName = "bankName",
                                additionalAccountIdentifiers = listOf(
                                    Submission.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                        id = "bankAccounts.additionalAccountIdentifiers.id",
                                        scheme = "bankAccounts.additionalAccountIdentifiers.scheme"
                                    )
                                ),
                                accountIdentification = Submission.Candidate.Details.BankAccount.AccountIdentification(
                                    id = "bankAccounts.accountIdentification.id",
                                    scheme = "bankAccounts.accountIdentification.scheme"
                                ),
                                address = Submission.Candidate.Details.BankAccount.Address(
                                    streetAddress = "streetAddress",
                                    postalCode = "postalCode",
                                    addressDetails = Submission.Candidate.Details.BankAccount.Address.AddressDetails(
                                        country = Submission.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                            id = "country.id",
                                            scheme = "country.scheme",
                                            description = "country.description"
                                        ),
                                        region = Submission.Candidate.Details.BankAccount.Address.AddressDetails.Region(
                                            id = "region.id",
                                            scheme = "region.scheme",
                                            description = "region.description"
                                        ),
                                        locality = Submission.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                            id = "locality.id",
                                            scheme = "locality.scheme",
                                            description = "locality.description"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
}