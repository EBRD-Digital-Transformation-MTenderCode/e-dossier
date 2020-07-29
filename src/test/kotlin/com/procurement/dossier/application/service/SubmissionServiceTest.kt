package com.procurement.dossier.application.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.data.submission.check.CheckAccessToSubmissionParams
import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsForOpeningParams
import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsForOpeningResult
import com.procurement.dossier.application.model.data.submission.get.GetSubmissionsByQualificationIdsParams
import com.procurement.dossier.application.model.data.submission.get.GetSubmissionsByQualificationIdsResult
import com.procurement.dossier.application.model.data.submission.organization.GetOrganizationsParams
import com.procurement.dossier.application.model.data.submission.organization.GetOrganizationsResult
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsParams
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsResult
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionParams
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionResult
import com.procurement.dossier.application.model.data.submission.validate.ValidateSubmissionParams
import com.procurement.dossier.application.repository.RulesRepository
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
import com.procurement.dossier.domain.model.qualification.QualificationId
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionCredentials
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.model.submission.SubmissionState
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.format
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
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
        private val COUNTRY = "MD"
        private val PMD = ProcurementMethod.GPA
        private val SUBMISSION_QUANTITY = 2L
        private val SUBMISSION_ID_1 = UUID.randomUUID()
        private val SUBMISSION_ID_2 = UUID.randomUUID()

        private val submissionRepository: SubmissionRepository = mock()
        private val rulesRepository: RulesRepository = mock()
        private val generable: Generable = mock()

        private val submissionService: SubmissionService = SubmissionService(
            submissionRepository = submissionRepository,
            rulesRepository = rulesRepository,
            generable = generable
        )
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
    inner class GetOrganizations {

        @Test
        fun success() {
            val params = getParams()

            val submission = stubSubmission()
            whenever(submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)).thenReturn(
                listOf(submission).asSuccess()
            )
            val actual = submissionService.getOrganizations(params).get
            val expected = submission.candidates.map { it.toGetOrganizationsResult() }.toList()

            assertEquals(expected, actual)
        }

        @Test
        fun twoSubmissions_success() {
            val params = getParams()

            val firstSubmission = stubSubmission()
            val secondSubmissionCandidates = stubSubmission().candidates
                .map { candidate -> candidate.copy(id = UUID.randomUUID().toString()) }
            val secondSubmission = stubSubmission().copy(candidates = secondSubmissionCandidates)

            val submissions = listOf(firstSubmission, secondSubmission)
            whenever(submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)).thenReturn(
                submissions.asSuccess()
            )
            val actual = submissionService.getOrganizations(params).get
            val expected = submissions.flatMap { it.candidates }.map { it.toGetOrganizationsResult() }.toList()

            assertEquals(expected, actual)
        }

        @Test
        fun noOrganizationPresent_fail() {
            val params = getParams()
            val submission = stubSubmission().copy(candidates = emptyList())
            whenever(submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)).thenReturn(
                listOf(submission).asSuccess()
            )
            val actualError = submissionService.getOrganizations(params).error

            assertTrue(actualError is ValidationErrors.OrganizationsNotFound)
        }

        @Test
        fun noSubmissionFound_fail() {
            val params = getParams()
            whenever(submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)).thenReturn(
                emptyList<Submission>().asSuccess()
            )
            val actualError = submissionService.getOrganizations(params).error

            assertTrue(actualError is ValidationErrors.RecordNotFoundFor.GetOrganizations)
        }

        private fun getParams() = GetOrganizationsParams.tryCreate(
            cpid = CPID.toString(),
            ocid = OCID.toString()
        ).get

        private fun Submission.Candidate.toGetOrganizationsResult() =
            GetOrganizationsResult(
                id = id,
                name = name,
                additionalIdentifiers = additionalIdentifiers.map { additionalIdentifier ->
                    GetOrganizationsResult.AdditionalIdentifier(
                        id = additionalIdentifier.id,
                        legalName = additionalIdentifier.legalName,
                        scheme = additionalIdentifier.scheme,
                        uri = additionalIdentifier.uri
                    )
                },
                address = address.let { address ->
                    GetOrganizationsResult.Address(
                        streetAddress = address.streetAddress,
                        postalCode = address.postalCode,
                        addressDetails = address.addressDetails.let { addressDetails ->
                            GetOrganizationsResult.Address.AddressDetails(
                                country = addressDetails.country.let { country ->
                                    GetOrganizationsResult.Address.AddressDetails.Country(
                                        id = country.id,
                                        scheme = country.scheme,
                                        description = country.description,
                                        uri = country.uri
                                    )
                                },
                                locality = addressDetails.locality.let { locality ->
                                    GetOrganizationsResult.Address.AddressDetails.Locality(
                                        id = locality.id,
                                        scheme = locality.scheme,
                                        description = locality.description,
                                        uri = locality.uri
                                    )
                                },
                                region = addressDetails.region.let { region ->
                                    GetOrganizationsResult.Address.AddressDetails.Region(
                                        id = region.id,
                                        scheme = region.scheme,
                                        description = region.description,
                                        uri = region.uri
                                    )
                                }
                            )
                        }
                    )

                },
                contactPoint = contactPoint.let { contactPoint ->
                    GetOrganizationsResult.ContactPoint(
                        name = contactPoint.name,
                        email = contactPoint.email,
                        faxNumber = contactPoint.faxNumber,
                        telephone = contactPoint.telephone,
                        url = contactPoint.url
                    )
                },
                details = details.let { details ->
                    GetOrganizationsResult.Details(
                        typeOfSupplier = details.typeOfSupplier,
                        bankAccounts = details.bankAccounts.map { bankAccount ->
                            GetOrganizationsResult.Details.BankAccount(
                                description = bankAccount.description,
                                address = bankAccount.address.let { address ->
                                    GetOrganizationsResult.Details.BankAccount.Address(
                                        streetAddress = address.streetAddress,
                                        postalCode = address.postalCode,
                                        addressDetails = address.addressDetails.let { addressDetails ->
                                            GetOrganizationsResult.Details.BankAccount.Address.AddressDetails(
                                                country = addressDetails.country.let { country ->
                                                    GetOrganizationsResult.Details.BankAccount.Address.AddressDetails.Country(
                                                        id = country.id,
                                                        scheme = country.scheme,
                                                        description = country.description
                                                    )
                                                },
                                                locality = addressDetails.locality.let { locality ->
                                                    GetOrganizationsResult.Details.BankAccount.Address.AddressDetails.Locality(
                                                        id = locality.id,
                                                        scheme = locality.scheme,
                                                        description = locality.description
                                                    )
                                                },
                                                region = addressDetails.region.let { region ->
                                                    GetOrganizationsResult.Details.BankAccount.Address.AddressDetails.Region(
                                                        id = region.id,
                                                        scheme = region.scheme,
                                                        description = region.description
                                                    )
                                                }
                                            )
                                        }
                                    )
                                },
                                accountIdentification = bankAccount.accountIdentification.let { accountIdentification ->
                                    GetOrganizationsResult.Details.BankAccount.AccountIdentification(
                                        id = accountIdentification.id,
                                        scheme = accountIdentification.scheme
                                    )
                                },
                                additionalAccountIdentifiers = bankAccount.additionalAccountIdentifiers.map { additionalAccountIdentifier ->
                                    GetOrganizationsResult.Details.BankAccount.AdditionalAccountIdentifier(
                                        id = additionalAccountIdentifier.id,
                                        scheme = additionalAccountIdentifier.scheme
                                    )
                                },
                                bankName = bankAccount.bankName,
                                identifier = bankAccount.identifier.let { identifier ->
                                    GetOrganizationsResult.Details.BankAccount.Identifier(
                                        id = identifier.id,
                                        scheme = identifier.scheme
                                    )
                                }
                            )
                        },
                        legalForm = details.legalForm?.let { legalForm ->
                            GetOrganizationsResult.Details.LegalForm(
                                id = legalForm.id,
                                scheme = legalForm.scheme,
                                description = legalForm.description,
                                uri = legalForm.uri
                            )
                        },
                        mainEconomicActivities = details.mainEconomicActivities.map { mainEconomicActivity ->
                            GetOrganizationsResult.Details.MainEconomicActivity(
                                id = mainEconomicActivity.id,
                                uri = mainEconomicActivity.uri,
                                description = mainEconomicActivity.description,
                                scheme = mainEconomicActivity.scheme
                            )
                        },
                        scale = details.scale
                    )
                },
                identifier = identifier.let { identifier ->
                    GetOrganizationsResult.Identifier(
                        id = identifier.id,
                        scheme = identifier.scheme,
                        uri = identifier.uri,
                        legalName = identifier.legalName
                    )
                },
                persones = persones.map { person ->
                    GetOrganizationsResult.Person(
                        id = person.id,
                        title = person.title,
                        identifier = person.identifier.let { identifier ->
                            GetOrganizationsResult.Person.Identifier(
                                id = identifier.id,
                                uri = identifier.uri,
                                scheme = identifier.scheme
                            )
                        },
                        name = person.name,
                        businessFunctions = person.businessFunctions.map { businessFunction ->
                            GetOrganizationsResult.Person.BusinessFunction(
                                id = businessFunction.id,
                                documents = businessFunction.documents.map { document ->
                                    GetOrganizationsResult.Person.BusinessFunction.Document(
                                        id = document.id,
                                        title = document.title,
                                        description = document.description,
                                        documentType = document.documentType
                                    )
                                },
                                jobTitle = businessFunction.jobTitle,
                                period = businessFunction.period.let { period ->
                                    GetOrganizationsResult.Person.BusinessFunction.Period(
                                        startDate = period.startDate
                                    )
                                },
                                type = businessFunction.type
                            )
                        }
                    )
                }
            )
    }

    @Nested
    inner class FindSubmissionsForOpening {

        @Test
        fun success() {
            val params = getParams()
            val firstSubmission = stubSubmission()
            val secondSubmission = stubSubmission()
            val submissions = listOf(firstSubmission, secondSubmission)
            whenever(rulesRepository.findSubmissionsMinimumQuantity(country = params.country, pmd = params.pmd))
                .thenReturn(SUBMISSION_QUANTITY.asSuccess())
            whenever(submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid))
                .thenReturn(submissions.asSuccess())

            val actual = submissionService.findSubmissionsForOpening(params).get
            val expected = submissions.map { it.toFindSubmissionsForOpeningResult() }

            assertEquals(expected, actual)
        }

        private fun getParams() = FindSubmissionsForOpeningParams.tryCreate(
            cpid = CPID.toString(),
            ocid = OCID.toString(),
            country = COUNTRY,
            pmd = PMD.name
        ).get

        private fun Submission.toFindSubmissionsForOpeningResult() = FindSubmissionsForOpeningResult(
            id = id,
            date = date,
            status = status,
            requirementResponses = requirementResponses.map { requirementResponse ->
                FindSubmissionsForOpeningResult.RequirementResponse(
                    id = requirementResponse.id,
                    relatedCandidate = requirementResponse.relatedCandidate.let { relatedCandidate ->
                        FindSubmissionsForOpeningResult.RequirementResponse.RelatedCandidate(
                            id = relatedCandidate.id,
                            name = relatedCandidate.name
                        )
                    },
                    requirement = requirementResponse.requirement.let { requirement ->
                        FindSubmissionsForOpeningResult.RequirementResponse.Requirement(
                            id = requirement.id
                        )
                    },
                    value = requirementResponse.value
                )
            },
            documents = documents.map { document ->
                FindSubmissionsForOpeningResult.Document(
                    id = document.id,
                    description = document.description,
                    documentType = document.documentType,
                    title = document.title
                )
            },
            candidates = candidates.map { candidate ->
                FindSubmissionsForOpeningResult.Candidate(
                    id = candidate.id,
                    name = candidate.name,
                    additionalIdentifiers = candidate.additionalIdentifiers.map { additionalIdentifier ->
                        FindSubmissionsForOpeningResult.Candidate.AdditionalIdentifier(
                            id = additionalIdentifier.id,
                            legalName = additionalIdentifier.legalName,
                            scheme = additionalIdentifier.scheme,
                            uri = additionalIdentifier.uri
                        )
                    },
                    address = candidate.address.let { address ->
                        FindSubmissionsForOpeningResult.Candidate.Address(
                            streetAddress = address.streetAddress,
                            postalCode = address.postalCode,
                            addressDetails = address.addressDetails.let { addressDetails ->
                                FindSubmissionsForOpeningResult.Candidate.Address.AddressDetails(
                                    country = addressDetails.country.let { country ->
                                        FindSubmissionsForOpeningResult.Candidate.Address.AddressDetails.Country(
                                            id = country.id,
                                            scheme = country.scheme,
                                            description = country.description,
                                            uri = country.uri
                                        )
                                    },
                                    locality = addressDetails.locality.let { locality ->
                                        FindSubmissionsForOpeningResult.Candidate.Address.AddressDetails.Locality(
                                            id = locality.id,
                                            scheme = locality.scheme,
                                            description = locality.description,
                                            uri = locality.uri
                                        )
                                    },
                                    region = addressDetails.region.let { region ->
                                        FindSubmissionsForOpeningResult.Candidate.Address.AddressDetails.Region(
                                            id = region.id,
                                            scheme = region.scheme,
                                            description = region.description,
                                            uri = region.uri
                                        )
                                    }
                                )
                            }
                        )

                    },
                    contactPoint = candidate.contactPoint.let { contactPoint ->
                        FindSubmissionsForOpeningResult.Candidate.ContactPoint(
                            name = contactPoint.name,
                            email = contactPoint.email,
                            faxNumber = contactPoint.faxNumber,
                            telephone = contactPoint.telephone,
                            url = contactPoint.url
                        )
                    },
                    details = candidate.details.let { details ->
                        FindSubmissionsForOpeningResult.Candidate.Details(
                            typeOfSupplier = details.typeOfSupplier,
                            bankAccounts = details.bankAccounts.map { bankAccount ->
                                FindSubmissionsForOpeningResult.Candidate.Details.BankAccount(
                                    description = bankAccount.description,
                                    address = bankAccount.address.let { address ->
                                        FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.Address(
                                            streetAddress = address.streetAddress,
                                            postalCode = address.postalCode,
                                            addressDetails = address.addressDetails.let { addressDetails ->
                                                FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.Address.AddressDetails(
                                                    country = addressDetails.country.let { country ->
                                                        FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                                            id = country.id,
                                                            scheme = country.scheme,
                                                            description = country.description
                                                        )
                                                    },
                                                    locality = addressDetails.locality.let { locality ->
                                                        FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                                            id = locality.id,
                                                            scheme = locality.scheme,
                                                            description = locality.description
                                                        )
                                                    },
                                                    region = addressDetails.region.let { region ->
                                                        FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.Address.AddressDetails.Region(
                                                            id = region.id,
                                                            scheme = region.scheme,
                                                            description = region.description
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    },
                                    accountIdentification = bankAccount.accountIdentification.let { accountIdentification ->
                                        FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.AccountIdentification(
                                            id = accountIdentification.id,
                                            scheme = accountIdentification.scheme
                                        )
                                    },
                                    additionalAccountIdentifiers = bankAccount.additionalAccountIdentifiers.map { additionalAccountIdentifier ->
                                        FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                            id = additionalAccountIdentifier.id,
                                            scheme = additionalAccountIdentifier.scheme
                                        )
                                    },
                                    bankName = bankAccount.bankName,
                                    identifier = bankAccount.identifier.let { identifier ->
                                        FindSubmissionsForOpeningResult.Candidate.Details.BankAccount.Identifier(
                                            id = identifier.id,
                                            scheme = identifier.scheme
                                        )
                                    }
                                )
                            },
                            legalForm = details.legalForm?.let { legalForm ->
                                FindSubmissionsForOpeningResult.Candidate.Details.LegalForm(
                                    id = legalForm.id,
                                    scheme = legalForm.scheme,
                                    description = legalForm.description,
                                    uri = legalForm.uri
                                )
                            },
                            mainEconomicActivities = details.mainEconomicActivities.map { mainEconomicActivity ->
                                FindSubmissionsForOpeningResult.Candidate.Details.MainEconomicActivity(
                                    id = mainEconomicActivity.id,
                                    uri = mainEconomicActivity.uri,
                                    description = mainEconomicActivity.description,
                                    scheme = mainEconomicActivity.scheme
                                )
                            },
                            scale = details.scale
                        )
                    },
                    identifier = candidate.identifier.let { identifier ->
                        FindSubmissionsForOpeningResult.Candidate.Identifier(
                            id = identifier.id,
                            scheme = identifier.scheme,
                            uri = identifier.uri,
                            legalName = identifier.legalName
                        )
                    },
                    persones = candidate.persones.map { person ->
                        FindSubmissionsForOpeningResult.Candidate.Person(
                            id = person.id,
                            title = person.title,
                            identifier = person.identifier.let { identifier ->
                                FindSubmissionsForOpeningResult.Candidate.Person.Identifier(
                                    id = identifier.id,
                                    uri = identifier.uri,
                                    scheme = identifier.scheme
                                )
                            },
                            name = person.name,
                            businessFunctions = person.businessFunctions.map { businessFunction ->
                                FindSubmissionsForOpeningResult.Candidate.Person.BusinessFunction(
                                    id = businessFunction.id,
                                    documents = businessFunction.documents.map { document ->
                                        FindSubmissionsForOpeningResult.Candidate.Person.BusinessFunction.Document(
                                            id = document.id,
                                            title = document.title,
                                            description = document.description,
                                            documentType = document.documentType
                                        )
                                    },
                                    jobTitle = businessFunction.jobTitle,
                                    period = businessFunction.period.let { period ->
                                        FindSubmissionsForOpeningResult.Candidate.Person.BusinessFunction.Period(
                                            startDate = period.startDate
                                        )
                                    },
                                    type = businessFunction.type
                                )
                            }
                        )
                    }
                )
            }
        )
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
        ) = ValidateSubmissionParams.tryCreate(
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

        private fun createPerson(businessFunctions: List<ValidateSubmissionParams.Candidate.Person.BusinessFunction>) =
            ValidateSubmissionParams.Candidate.Person.tryCreate(
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

        private fun createBusinessFunctionDocument(id: DocumentId) =
            ValidateSubmissionParams.Candidate.Person.BusinessFunction.Document.tryCreate(
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

    @Nested
    inner class GetSubmissionsByQualificationIds {

        @Test
        fun success() {
            val params = getParams()

            val submissionIds = params.qualifications.map { it.relatedSubmission }
            val storedSubmissions = listOf(
                stubSubmission().copy(id = submissionIds[0]),
                stubSubmission().copy(id = submissionIds[1])
            )
            whenever(submissionRepository.findBy(cpid = CPID, ocid = OCID, submissionIds = submissionIds))
                .thenReturn(storedSubmissions.asSuccess())

            val expected = storedSubmissions.convertToResult()
            val actual = submissionService.getSubmissionsByQualificationIds(params = params).get

            assertEquals(expected, actual)
        }

        @Test
        fun oneSubmissionNotFound_fail() {
            val params = getParams()

            val requestSubmissionIds = params.qualifications.map { it.relatedSubmission }
            val storedSubmissions = listOf(stubSubmission().copy(id = requestSubmissionIds[0]))
            whenever(submissionRepository.findBy(cpid = CPID, ocid = OCID, submissionIds = requestSubmissionIds))
                .thenReturn(storedSubmissions.asSuccess())

            val actual = submissionService.getSubmissionsByQualificationIds(params = params).error

            val expectedErrorCode = "VR.COM-5.17.1"
            val expectedDescription = "Submission id(s) '${requestSubmissionIds[1]}' not found."

            assertEquals(expectedErrorCode, actual.code)
            assertEquals(expectedDescription, actual.description)
        }

        @Test
        fun noSubmissionFound_fail() {
            val params = getParams()
            val requestSubmissionIds = params.qualifications.map { it.relatedSubmission }

            whenever(submissionRepository.findBy(cpid = CPID, ocid = OCID, submissionIds = requestSubmissionIds))
                .thenReturn(emptyList<Submission>().asSuccess())

            val actual = submissionService.getSubmissionsByQualificationIds(params = params).error

            val expectedErrorCode = "VR.COM-5.17.1"
            val expectedDescription = "Submission id(s) '${requestSubmissionIds[0]}, ${requestSubmissionIds[1]}' not found."

            assertEquals(expectedErrorCode, actual.code)
            assertEquals(expectedDescription, actual.description)
        }

        private fun List<Submission>.convertToResult() =
            GetSubmissionsByQualificationIdsResult(
                submissions = GetSubmissionsByQualificationIdsResult.Submissions(
                    details = this.map { submission ->
                        GetSubmissionsByQualificationIdsResult.Submissions.Detail(
                            id = submission.id,
                            date = submission.date,
                            status = submission.status,
                            requirementResponses = submission.requirementResponses.map { requirementResponse ->
                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.RequirementResponse(
                                    id = requirementResponse.id,
                                    relatedCandidate = requirementResponse.relatedCandidate.let { relatedCandidate ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.RequirementResponse.RelatedCandidate(
                                            id = relatedCandidate.id,
                                            name = relatedCandidate.name
                                        )
                                    },
                                    requirement = requirementResponse.requirement.let { requirement ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.RequirementResponse.Requirement(
                                            id = requirement.id
                                        )
                                    },
                                    value = requirementResponse.value
                                )
                            },
                            documents = submission.documents.map { document ->
                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Document(
                                    id = document.id,
                                    description = document.description,
                                    documentType = document.documentType,
                                    title = document.title
                                )
                            },
                            candidates = submission.candidates.map { candidate ->
                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate(
                                    id = candidate.id,
                                    name = candidate.name,
                                    additionalIdentifiers = candidate.additionalIdentifiers.map { additionalIdentifier ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.AdditionalIdentifier(
                                            id = additionalIdentifier.id,
                                            legalName = additionalIdentifier.legalName,
                                            scheme = additionalIdentifier.scheme,
                                            uri = additionalIdentifier.uri
                                        )
                                    },
                                    address = candidate.address.let { address ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Address(
                                            streetAddress = address.streetAddress,
                                            postalCode = address.postalCode,
                                            addressDetails = address.addressDetails.let { addressDetails ->
                                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Address.AddressDetails(
                                                    country = addressDetails.country.let { country ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Address.AddressDetails.Country(
                                                            id = country.id,
                                                            scheme = country.scheme,
                                                            description = country.description,
                                                            uri = country.uri
                                                        )
                                                    },
                                                    locality = addressDetails.locality.let { locality ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Address.AddressDetails.Locality(
                                                            id = locality.id,
                                                            scheme = locality.scheme,
                                                            description = locality.description,
                                                            uri = locality.uri
                                                        )
                                                    },
                                                    region = addressDetails.region.let { region ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Address.AddressDetails.Region(
                                                            id = region.id,
                                                            scheme = region.scheme,
                                                            description = region.description,
                                                            uri = region.uri
                                                        )
                                                    }
                                                )
                                            }
                                        )

                                    },
                                    contactPoint = candidate.contactPoint.let { contactPoint ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.ContactPoint(
                                            name = contactPoint.name,
                                            email = contactPoint.email,
                                            faxNumber = contactPoint.faxNumber,
                                            telephone = contactPoint.telephone,
                                            url = contactPoint.url
                                        )
                                    },
                                    details = candidate.details.let { details ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details(
                                            typeOfSupplier = details.typeOfSupplier,
                                            bankAccounts = details.bankAccounts.map { bankAccount ->
                                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount(
                                                    description = bankAccount.description,
                                                    address = bankAccount.address.let { address ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.Address(
                                                            streetAddress = address.streetAddress,
                                                            postalCode = address.postalCode,
                                                            addressDetails = address.addressDetails.let { addressDetails ->
                                                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.Address.AddressDetails(
                                                                    country = addressDetails.country.let { country ->
                                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                                                            id = country.id,
                                                                            scheme = country.scheme,
                                                                            description = country.description
                                                                        )
                                                                    },
                                                                    locality = addressDetails.locality.let { locality ->
                                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                                                            id = locality.id,
                                                                            scheme = locality.scheme,
                                                                            description = locality.description
                                                                        )
                                                                    },
                                                                    region = addressDetails.region.let { region ->
                                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.Address.AddressDetails.Region(
                                                                            id = region.id,
                                                                            scheme = region.scheme,
                                                                            description = region.description
                                                                        )
                                                                    }
                                                                )
                                                            }
                                                        )
                                                    },
                                                    accountIdentification = bankAccount.accountIdentification.let { accountIdentification ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.AccountIdentification(
                                                            id = accountIdentification.id,
                                                            scheme = accountIdentification.scheme
                                                        )
                                                    },
                                                    additionalAccountIdentifiers = bankAccount.additionalAccountIdentifiers.map { additionalAccountIdentifier ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                                            id = additionalAccountIdentifier.id,
                                                            scheme = additionalAccountIdentifier.scheme
                                                        )
                                                    },
                                                    bankName = bankAccount.bankName,
                                                    identifier = bankAccount.identifier.let { identifier ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.BankAccount.Identifier(
                                                            id = identifier.id,
                                                            scheme = identifier.scheme
                                                        )
                                                    }
                                                )
                                            },
                                            legalForm = details.legalForm?.let { legalForm ->
                                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.LegalForm(
                                                    id = legalForm.id,
                                                    scheme = legalForm.scheme,
                                                    description = legalForm.description,
                                                    uri = legalForm.uri
                                                )
                                            },
                                            mainEconomicActivities = details.mainEconomicActivities.map { mainEconomicActivity ->
                                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Details.MainEconomicActivity(
                                                    id = mainEconomicActivity.id,
                                                    uri = mainEconomicActivity.uri,
                                                    description = mainEconomicActivity.description,
                                                    scheme = mainEconomicActivity.scheme
                                                )
                                            },
                                            scale = details.scale
                                        )
                                    },
                                    identifier = candidate.identifier.let { identifier ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Identifier(
                                            id = identifier.id,
                                            scheme = identifier.scheme,
                                            uri = identifier.uri,
                                            legalName = identifier.legalName
                                        )
                                    },
                                    persones = candidate.persones.map { person ->
                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Person(
                                            id = person.id,
                                            title = person.title,
                                            identifier = person.identifier.let { identifier ->
                                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Person.Identifier(
                                                    id = identifier.id,
                                                    uri = identifier.uri,
                                                    scheme = identifier.scheme
                                                )
                                            },
                                            name = person.name,
                                            businessFunctions = person.businessFunctions.map { businessFunction ->
                                                GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Person.BusinessFunction(
                                                    id = businessFunction.id,
                                                    documents = businessFunction.documents.map { document ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Person.BusinessFunction.Document(
                                                            id = document.id,
                                                            title = document.title,
                                                            description = document.description,
                                                            documentType = document.documentType
                                                        )
                                                    },
                                                    jobTitle = businessFunction.jobTitle,
                                                    period = businessFunction.period.let { period ->
                                                        GetSubmissionsByQualificationIdsResult.Submissions.Detail.Candidate.Person.BusinessFunction.Period(
                                                            startDate = period.startDate
                                                        )
                                                    },
                                                    type = businessFunction.type
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            )

        private fun getParams() = GetSubmissionsByQualificationIdsParams.tryCreate(
            cpid = CPID.toString(),
            ocid = OCID.toString(),
            qualifications = listOf(
                GetSubmissionsByQualificationIdsParams.Qualification.tryCreate(
                    id = QualificationId.generate().toString(),
                    relatedSubmission = UUID.randomUUID().toString()
                ).get,
                GetSubmissionsByQualificationIdsParams.Qualification.tryCreate(
                    id = QualificationId.generate().toString(),
                    relatedSubmission = UUID.randomUUID().toString()
                ).get
            )
        ).get
    }

    private fun stubSubmission() =
        Submission(
            id = SubmissionId.create(UUID.randomUUID().toString()),
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