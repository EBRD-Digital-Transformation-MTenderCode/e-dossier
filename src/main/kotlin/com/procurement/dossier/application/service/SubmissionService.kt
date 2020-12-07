package com.procurement.dossier.application.service

import com.procurement.dossier.application.model.data.submission.check.CheckAccessToSubmissionParams
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionParams
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionResult
import com.procurement.dossier.application.model.data.submission.finalize.FinalizeSubmissionsParams
import com.procurement.dossier.application.model.data.submission.finalize.FinalizeSubmissionsResult
import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsParams
import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsResult
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
import com.procurement.dossier.domain.fail.error.ValidationErrors.EntityNotFound
import com.procurement.dossier.domain.fail.error.ValidationErrors.SubmissionNotFoundFor
import com.procurement.dossier.domain.fail.error.ValidationErrors.SubmissionsNotFoundFor
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.qualification.QualificationStatus
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.Result.Companion.success
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.doOnFalse
import com.procurement.dossier.domain.util.extension.getDuplicate
import com.procurement.dossier.domain.util.extension.getUnknownElements
import com.procurement.dossier.domain.util.extension.toSetBy
import com.procurement.dossier.infrastructure.converter.submission.toCreateSubmissionResult
import org.springframework.stereotype.Service

@Service
class SubmissionService(
    private val submissionRepository: SubmissionRepository,
    private val rulesRepository: RulesRepository,
    private val generable: Generable
) {
    fun createSubmission(params: CreateSubmissionParams): Result<CreateSubmissionResult, Fail.Incident> {
        val storedSubmissions = submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)
            .orForwardFail { return it }

        val receivedCandidates = params.submission.candidates.toSetBy { it.id }
        val submissionsToWithdraw = storedSubmissions
            .filter { submission ->  submission.status == SubmissionStatus.PENDING }
            .filter { submission -> isSubmittedByReceivedCandidates(submission, receivedCandidates) }
            .map { submission -> submission.copy(status = SubmissionStatus.WITHDRAWN) }

        val createdSubmission = params.convert()

        val submissions = submissionsToWithdraw + createdSubmission
        submissionRepository.saveAll(cpid = params.cpid, ocid = params.ocid, submissions = submissions)
            .doOnFail { incident -> return incident.asFailure() }
        return createdSubmission.toCreateSubmissionResult().asSuccess()
    }

    private fun isSubmittedByReceivedCandidates(
        submission: Submission,
        receivedCandidates: Set<String>
    ): Boolean {
        val storedCandidates = submission.candidates.toSetBy { it.id }
        return storedCandidates == receivedCandidates
    }

    fun finalizeSubmissions(params: FinalizeSubmissionsParams): Result<FinalizeSubmissionsResult, Fail> {

        val submissionsFromDb = submissionRepository
            .findBy(cpid = params.cpid, ocid = params.ocid)
            .map { submissions -> submissions.takeIf { it.isNotEmpty() } }
            .orForwardFail { fail -> return fail }
            ?: return failure(SubmissionsNotFoundFor.FinalizeSubmissions(params.cpid, params.ocid))

        val submissionFromDbById = submissionsFromDb.associateBy { it.id }

        val receivedRelatedSubmission = params.qualifications.map { it.relatedSubmission }
        val missingSubmissions = submissionFromDbById.keys.getUnknownElements(receivedRelatedSubmission)
        if (missingSubmissions.isNotEmpty())
            return failure(SubmissionNotFoundFor.FinalizeSubmission(missingSubmissions))

        checkMissingSubmission(available = submissionFromDbById.keys, received = receivedRelatedSubmission)
            .orForwardFail { error -> return error }

        val receivedQualificationById = params.qualifications.associateBy { it.relatedSubmission }

        val updatedSubmissions = submissionsFromDb
            .filter { it.status == SubmissionStatus.PENDING }
            .map { submission ->
                val definedStatus = defineStatusToUpdate(receivedQualificationById[submission.id]?.status)
                submission.copy(status = definedStatus)
            }

        val result = FinalizeSubmissionsResult(
            submissions = FinalizeSubmissionsResult.Submissions(
                details = updatedSubmissions.map { FinalizeSubmissionsResult.fromDomain(it) }
            )
        )

        submissionRepository.saveAll(params.cpid, params.ocid, updatedSubmissions)

        return success(result)
    }

    fun checkMissingSubmission(available: Collection<SubmissionId>, received: Collection<SubmissionId>)
    : Result<Collection<SubmissionId>, SubmissionNotFoundFor.FinalizeSubmission> {
        val missingSubmissions = received.getUnknownElements(received)
        return if (missingSubmissions.isNotEmpty())
            failure(SubmissionNotFoundFor.FinalizeSubmission(missingSubmissions))
        else
            success(missingSubmissions)
    }

    fun defineStatusToUpdate(status: QualificationStatus?): SubmissionStatus = when (status) {
        QualificationStatus.ACTIVE -> SubmissionStatus.VALID
        QualificationStatus.UNSUCCESSFUL -> SubmissionStatus.DISQUALIFIED
        null -> SubmissionStatus.WITHDRAWN
    }

    private fun CreateSubmissionParams.convert() =
        Submission(
            id = submission.id,
            date = date,
            status = SubmissionStatus.PENDING,
            token = generable.generateToken(),
            owner = owner,
            requirementResponses = submission.requirementResponses.map { requirementResponse ->
                Submission.RequirementResponse(
                    id = requirementResponse.id,
                    relatedCandidate = requirementResponse.relatedCandidate.let { relatedCandidate ->
                        Submission.RequirementResponse.RelatedCandidate(
                            id = relatedCandidate.id,
                            name = relatedCandidate.name
                        )
                    },
                    requirement = requirementResponse.requirement.let { requirement ->
                        Submission.RequirementResponse.Requirement(
                            id = requirement.id
                        )
                    },
                    value = requirementResponse.value
                )
            },
            documents = submission.documents.map { document ->
                Submission.Document(
                    id = document.id,
                    description = document.description,
                    documentType = document.documentType,
                    title = document.title
                )
            },
            candidates = submission.candidates.map { candidate ->
                Submission.Candidate(
                    id = candidate.id,
                    name = candidate.name,
                    additionalIdentifiers = candidate.additionalIdentifiers.map { additionalIdentifier ->
                        Submission.Candidate.AdditionalIdentifier(
                            id = additionalIdentifier.id,
                            legalName = additionalIdentifier.legalName,
                            scheme = additionalIdentifier.scheme,
                            uri = additionalIdentifier.uri
                        )
                    },
                    address = candidate.address.let { address ->
                        Submission.Candidate.Address(
                            streetAddress = address.streetAddress,
                            postalCode = address.postalCode,
                            addressDetails = address.addressDetails.let { addressDetails ->
                                Submission.Candidate.Address.AddressDetails(
                                    country = addressDetails.country.let { country ->
                                        Submission.Candidate.Address.AddressDetails.Country(
                                            id = country.id,
                                            scheme = country.scheme,
                                            description = country.description,
                                            uri = country.uri
                                        )
                                    },
                                    locality = addressDetails.locality.let { locality ->
                                        Submission.Candidate.Address.AddressDetails.Locality(
                                            id = locality.id,
                                            scheme = locality.scheme,
                                            description = locality.description,
                                            uri = locality.uri
                                        )
                                    },
                                    region = addressDetails.region.let { region ->
                                        Submission.Candidate.Address.AddressDetails.Region(
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
                        Submission.Candidate.ContactPoint(
                            name = contactPoint.name,
                            email = contactPoint.email,
                            faxNumber = contactPoint.faxNumber,
                            telephone = contactPoint.telephone,
                            url = contactPoint.url
                        )
                    },
                    details = candidate.details.let { details ->
                        Submission.Candidate.Details(
                            typeOfSupplier = details.typeOfSupplier,
                            bankAccounts = details.bankAccounts.map { bankAccount ->
                                Submission.Candidate.Details.BankAccount(
                                    description = bankAccount.description,
                                    address = bankAccount.address.let { address ->
                                        Submission.Candidate.Details.BankAccount.Address(
                                            streetAddress = address.streetAddress,
                                            postalCode = address.postalCode,
                                            addressDetails = address.addressDetails.let { addressDetails ->
                                                Submission.Candidate.Details.BankAccount.Address.AddressDetails(
                                                    country = addressDetails.country.let { country ->
                                                        Submission.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                                            id = country.id,
                                                            scheme = country.scheme,
                                                            description = country.description
                                                        )
                                                    },
                                                    locality = addressDetails.locality.let { locality ->
                                                        Submission.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                                            id = locality.id,
                                                            scheme = locality.scheme,
                                                            description = locality.description
                                                        )
                                                    },
                                                    region = addressDetails.region.let { region ->
                                                        Submission.Candidate.Details.BankAccount.Address.AddressDetails.Region(
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
                                        Submission.Candidate.Details.BankAccount.AccountIdentification(
                                            id = accountIdentification.id,
                                            scheme = accountIdentification.scheme
                                        )
                                    },
                                    additionalAccountIdentifiers = bankAccount.additionalAccountIdentifiers.map { additionalAccountIdentifier ->
                                        Submission.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                            id = additionalAccountIdentifier.id,
                                            scheme = additionalAccountIdentifier.scheme
                                        )
                                    },
                                    bankName = bankAccount.bankName,
                                    identifier = bankAccount.identifier.let { identifier ->
                                        Submission.Candidate.Details.BankAccount.Identifier(
                                            id = identifier.id,
                                            scheme = identifier.scheme
                                        )
                                    }
                                )
                            },
                            legalForm = details.legalForm?.let { legalForm ->
                                Submission.Candidate.Details.LegalForm(
                                    id = legalForm.id,
                                    scheme = legalForm.scheme,
                                    description = legalForm.description,
                                    uri = legalForm.uri
                                )
                            },
                            mainEconomicActivities = details.mainEconomicActivities.map { mainEconomicActivity ->
                                Submission.Candidate.Details.MainEconomicActivity(
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
                        Submission.Candidate.Identifier(
                            id = identifier.id,
                            scheme = identifier.scheme,
                            uri = identifier.uri,
                            legalName = identifier.legalName
                        )
                    },
                    persones = candidate.persones.map { person ->
                        Submission.Candidate.Person(
                            id = person.id,
                            title = person.title,
                            identifier = person.identifier.let { identifier ->
                                Submission.Candidate.Person.Identifier(
                                    id = identifier.id,
                                    uri = identifier.uri,
                                    scheme = identifier.scheme
                                )
                            },
                            name = person.name,
                            businessFunctions = person.businessFunctions.map { businessFunction ->
                                Submission.Candidate.Person.BusinessFunction(
                                    id = businessFunction.id,
                                    documents = businessFunction.documents.map { document ->
                                        Submission.Candidate.Person.BusinessFunction.Document(
                                            id = document.id,
                                            title = document.title,
                                            description = document.description,
                                            documentType = document.documentType
                                        )
                                    },
                                    jobTitle = businessFunction.jobTitle,
                                    period = businessFunction.period.let { period ->
                                        Submission.Candidate.Person.BusinessFunction.Period(
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

    fun getSubmissionStateByIds(params: GetSubmissionStateByIdsParams): Result<List<GetSubmissionStateByIdsResult>, Fail> {
        val states = submissionRepository.getSubmissionsStates(
            cpid = params.cpid, ocid = params.ocid, submissionIds = params.submissionIds
        ).orForwardFail { fail -> return fail }

        checkForUnknownElements(received = params.submissionIds, known = states.map { it.id })
            .doOnError { error -> return error.asFailure() }

        return states.map { state -> GetSubmissionStateByIdsResult(id = state.id, status = state.status) }.asSuccess()
    }

    private fun checkForUnknownElements(
        received: List<SubmissionId>, known: List<SubmissionId>
    ): ValidationResult<SubmissionNotFoundFor> {
        val unknownElements = known.getUnknownElements(received = received)
        return if (unknownElements.isNotEmpty())
            ValidationResult.error(SubmissionNotFoundFor.GetSubmissionStateByIds(unknownElements.first()))
        else ValidationResult.ok()
    }

    fun setStateForSubmission(params: SetStateForSubmissionParams): Result<SetStateForSubmissionResult, Fail> {
        val requestSubmission = params.submission

        val storedSubmission = submissionRepository.findSubmission(
            cpid = params.cpid, ocid = params.ocid, id = requestSubmission.id
        )
            .orForwardFail { fail -> return fail }
            ?: return SubmissionNotFoundFor.SetStateForSubmission(id = requestSubmission.id)
                .asFailure()

        val updatedSubmission = storedSubmission.copy(status = requestSubmission.status)

        submissionRepository.updateSubmission(
            cpid = params.cpid, ocid = params.ocid, submission = updatedSubmission
        ).orForwardFail { fail -> return fail }
            .doOnFalse {
                return Fail.Incident.Database.Consistency("Could not update submission '${updatedSubmission.id}'")
                    .asFailure()
            }
        return SetStateForSubmissionResult(id = updatedSubmission.id, status = updatedSubmission.status).asSuccess()
    }

    fun checkAccessToSubmission(params: CheckAccessToSubmissionParams): ValidationResult<Fail> {
        val credentials = submissionRepository.getSubmissionCredentials(
            cpid = params.cpid, ocid = params.ocid, id = params.submissionId
        ).doReturn { incident -> return ValidationResult.error(incident) }
            ?: return ValidationResult.error(SubmissionNotFoundFor.CheckAccessToSubmission(id = params.submissionId))

        if (params.token != credentials.token)
            return ValidationResult.error(ValidationErrors.InvalidToken())

        if (params.owner != credentials.owner)
            return ValidationResult.error(ValidationErrors.InvalidOwner())

        return ValidationResult.ok()
    }

    fun getOrganizations(params: GetOrganizationsParams): Result<List<GetOrganizationsResult>, Fail> {
        val submissions = submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)
            .orForwardFail { fail -> return fail }
        if (submissions.isEmpty())
            return ValidationErrors.RecordNotFoundFor.GetOrganizations(cpid = params.cpid, ocid = params.ocid)
                .asFailure()

        val organizations = submissions
            .asSequence()
            .flatMap { submission -> submission.candidates.asSequence() }
            .map { candidate -> candidate.toGetOrganizationsResult() }
            .toList()

        if (organizations.isEmpty())
            return ValidationErrors.OrganizationsNotFound(cpid = params.cpid, ocid = params.ocid).asFailure()

        return organizations.asSuccess()
    }

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

    fun findSubmissions(params: FindSubmissionsParams): Result<List<FindSubmissionsResult>, Fail> {
        val submissions = submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)
            .orForwardFail { fail -> return fail }

        if (submissions.isEmpty())
            return emptyList<FindSubmissionsResult>().asSuccess()

        val validStatus = rulesRepository.findSubmissionValidState(params.country, params.pmd, params.operationType)
            .orForwardFail { fail -> return fail }
            ?: return EntityNotFound.SubmissionValidStateRule(params.country, params.pmd, params.operationType)
                .asFailure()

        val validSubmissions = submissions.filter { submission -> submission.status == validStatus }

        val minimumQuantity =
            rulesRepository.findSubmissionsMinimumQuantity(
                country = params.country,
                pmd = params.pmd,
                operationType = params.operationType
            )
                .orForwardFail { return it }
                ?: return EntityNotFound.SubmissionMinimumQuantityRule(params.country, params.pmd, params.operationType)
                    .asFailure()

        return if (validSubmissions.size >= minimumQuantity)
            validSubmissions
                .map { submission -> submission.toFindSubmissionsForOpeningResult() }
                .asSuccess()
        else
            emptyList<FindSubmissionsResult>().asSuccess()
    }

    private fun Submission.toFindSubmissionsForOpeningResult() = FindSubmissionsResult(
        id = id,
        date = date,
        status = status,
        requirementResponses = requirementResponses.map { requirementResponse ->
            FindSubmissionsResult.RequirementResponse(
                id = requirementResponse.id,
                relatedCandidate = requirementResponse.relatedCandidate.let { relatedCandidate ->
                    FindSubmissionsResult.RequirementResponse.RelatedCandidate(
                        id = relatedCandidate.id,
                        name = relatedCandidate.name
                    )
                },
                requirement = requirementResponse.requirement.let { requirement ->
                    FindSubmissionsResult.RequirementResponse.Requirement(
                        id = requirement.id
                    )
                },
                value = requirementResponse.value
            )
        },
        documents = documents.map { document ->
            FindSubmissionsResult.Document(
                id = document.id,
                description = document.description,
                documentType = document.documentType,
                title = document.title
            )
        },
        candidates = candidates.map { candidate ->
            FindSubmissionsResult.Candidate(
                id = candidate.id,
                name = candidate.name,
                additionalIdentifiers = candidate.additionalIdentifiers.map { additionalIdentifier ->
                    FindSubmissionsResult.Candidate.AdditionalIdentifier(
                        id = additionalIdentifier.id,
                        legalName = additionalIdentifier.legalName,
                        scheme = additionalIdentifier.scheme,
                        uri = additionalIdentifier.uri
                    )
                },
                address = candidate.address.let { address ->
                    FindSubmissionsResult.Candidate.Address(
                        streetAddress = address.streetAddress,
                        postalCode = address.postalCode,
                        addressDetails = address.addressDetails.let { addressDetails ->
                            FindSubmissionsResult.Candidate.Address.AddressDetails(
                                country = addressDetails.country.let { country ->
                                    FindSubmissionsResult.Candidate.Address.AddressDetails.Country(
                                        id = country.id,
                                        scheme = country.scheme,
                                        description = country.description,
                                        uri = country.uri
                                    )
                                },
                                locality = addressDetails.locality.let { locality ->
                                    FindSubmissionsResult.Candidate.Address.AddressDetails.Locality(
                                        id = locality.id,
                                        scheme = locality.scheme,
                                        description = locality.description,
                                        uri = locality.uri
                                    )
                                },
                                region = addressDetails.region.let { region ->
                                    FindSubmissionsResult.Candidate.Address.AddressDetails.Region(
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
                    FindSubmissionsResult.Candidate.ContactPoint(
                        name = contactPoint.name,
                        email = contactPoint.email,
                        faxNumber = contactPoint.faxNumber,
                        telephone = contactPoint.telephone,
                        url = contactPoint.url
                    )
                },
                details = candidate.details.let { details ->
                    FindSubmissionsResult.Candidate.Details(
                        typeOfSupplier = details.typeOfSupplier,
                        bankAccounts = details.bankAccounts.map { bankAccount ->
                            FindSubmissionsResult.Candidate.Details.BankAccount(
                                description = bankAccount.description,
                                address = bankAccount.address.let { address ->
                                    FindSubmissionsResult.Candidate.Details.BankAccount.Address(
                                        streetAddress = address.streetAddress,
                                        postalCode = address.postalCode,
                                        addressDetails = address.addressDetails.let { addressDetails ->
                                            FindSubmissionsResult.Candidate.Details.BankAccount.Address.AddressDetails(
                                                country = addressDetails.country.let { country ->
                                                    FindSubmissionsResult.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                                        id = country.id,
                                                        scheme = country.scheme,
                                                        description = country.description
                                                    )
                                                },
                                                locality = addressDetails.locality.let { locality ->
                                                    FindSubmissionsResult.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                                        id = locality.id,
                                                        scheme = locality.scheme,
                                                        description = locality.description
                                                    )
                                                },
                                                region = addressDetails.region.let { region ->
                                                    FindSubmissionsResult.Candidate.Details.BankAccount.Address.AddressDetails.Region(
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
                                    FindSubmissionsResult.Candidate.Details.BankAccount.AccountIdentification(
                                        id = accountIdentification.id,
                                        scheme = accountIdentification.scheme
                                    )
                                },
                                additionalAccountIdentifiers = bankAccount.additionalAccountIdentifiers.map { additionalAccountIdentifier ->
                                    FindSubmissionsResult.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                        id = additionalAccountIdentifier.id,
                                        scheme = additionalAccountIdentifier.scheme
                                    )
                                },
                                bankName = bankAccount.bankName,
                                identifier = bankAccount.identifier.let { identifier ->
                                    FindSubmissionsResult.Candidate.Details.BankAccount.Identifier(
                                        id = identifier.id,
                                        scheme = identifier.scheme
                                    )
                                }
                            )
                        },
                        legalForm = details.legalForm?.let { legalForm ->
                            FindSubmissionsResult.Candidate.Details.LegalForm(
                                id = legalForm.id,
                                scheme = legalForm.scheme,
                                description = legalForm.description,
                                uri = legalForm.uri
                            )
                        },
                        mainEconomicActivities = details.mainEconomicActivities.map { mainEconomicActivity ->
                            FindSubmissionsResult.Candidate.Details.MainEconomicActivity(
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
                    FindSubmissionsResult.Candidate.Identifier(
                        id = identifier.id,
                        scheme = identifier.scheme,
                        uri = identifier.uri,
                        legalName = identifier.legalName
                    )
                },
                persones = candidate.persones.map { person ->
                    FindSubmissionsResult.Candidate.Person(
                        id = person.id,
                        title = person.title,
                        identifier = person.identifier.let { identifier ->
                            FindSubmissionsResult.Candidate.Person.Identifier(
                                id = identifier.id,
                                uri = identifier.uri,
                                scheme = identifier.scheme
                            )
                        },
                        name = person.name,
                        businessFunctions = person.businessFunctions.map { businessFunction ->
                            FindSubmissionsResult.Candidate.Person.BusinessFunction(
                                id = businessFunction.id,
                                documents = businessFunction.documents.map { document ->
                                    FindSubmissionsResult.Candidate.Person.BusinessFunction.Document(
                                        id = document.id,
                                        title = document.title,
                                        description = document.description,
                                        documentType = document.documentType
                                    )
                                },
                                jobTitle = businessFunction.jobTitle,
                                period = businessFunction.period.let { period ->
                                    FindSubmissionsResult.Candidate.Person.BusinessFunction.Period(
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

    fun validateSubmission(params: ValidateSubmissionParams): ValidationResult<Fail> {
        val duplicateCandidate = params.candidates.getDuplicate { it.id }
        if (duplicateCandidate != null)
            return ValidationResult.error(ValidationErrors.Duplicate.Candidate(id = duplicateCandidate.id))

        val duplicateDocument = params.documents.getDuplicate { it.id }
        if (duplicateDocument != null)
            return ValidationResult.error(ValidationErrors.Duplicate.OrganizationDocument(id = duplicateDocument.id))

        checkDuplicatesWithinPerson(params)
            .doOnError { return ValidationResult.error(it) }

        return ValidationResult.ok()
    }

    private fun checkDuplicatesWithinPerson(params: ValidateSubmissionParams): ValidationResult<Fail> {
        val persons = params.candidates
            .asSequence()
            .flatMap { candidate -> candidate.persones.asSequence() }

        val duplicateBusinessFunction = getDuplicateBusinessFunctionWithinPerson(persons)
        if (duplicateBusinessFunction != null)
            return ValidationResult.error(ValidationErrors.Duplicate.PersonBusinessFunction(id = duplicateBusinessFunction.id))

        val duplicatePersonDocument = getDuplicateDocumentWithinPerson(persons)
        if (duplicatePersonDocument != null)
            return ValidationResult.error(ValidationErrors.Duplicate.PersonDocument(id = duplicatePersonDocument.id))

        return ValidationResult.ok()
    }

    private fun getDuplicateBusinessFunctionWithinPerson(persons: Sequence<ValidateSubmissionParams.Candidate.Person>) =
        persons.map { person -> person.businessFunctions.getDuplicate { it.id } }
            .firstOrNull { duplicate -> duplicate != null }

    private fun getDuplicateDocumentWithinPerson(persons: Sequence<ValidateSubmissionParams.Candidate.Person>) =
        persons.map { person ->
            person.businessFunctions
                .flatMap { businessFunction -> businessFunction.documents }
                .getDuplicate { document -> document.id }
        }
            .firstOrNull { duplicate -> duplicate != null }

    fun getSubmissionsByQualificationIds(params: GetSubmissionsByQualificationIdsParams)
        : Result<GetSubmissionsByQualificationIdsResult, Fail> {
        val submissionIds = params.qualifications.map { it.relatedSubmission }
        val submissions = submissionRepository.findBy(
            cpid = params.cpid, ocid = params.ocid, submissionIds = submissionIds
        ).orForwardFail { fail -> return fail }

        val unknownSubmissions = submissions.map { it.id }
            .getUnknownElements(received = submissionIds)

        if (unknownSubmissions.isNotEmpty())
            return SubmissionNotFoundFor.SubmissionsByQualificationIds(ids = unknownSubmissions.toList())
                .asFailure()

        return GetSubmissionsByQualificationIdsResult(
            submissions = GetSubmissionsByQualificationIdsResult.Submissions(
                details = submissions.map { submission ->
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
        ).asSuccess()
    }
}