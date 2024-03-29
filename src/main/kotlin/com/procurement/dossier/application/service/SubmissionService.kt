package com.procurement.dossier.application.service

import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.PersonesProcessingParams
import com.procurement.dossier.application.model.data.errors.GetInvitedCandidatesOwnersErrors
import com.procurement.dossier.application.model.data.submission.check.CheckAccessToSubmissionParams
import com.procurement.dossier.application.model.data.submission.check.CheckPresenceCandidateInOneSubmissionParams
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionParams
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionResult
import com.procurement.dossier.application.model.data.submission.finalize.FinalizeSubmissionsParams
import com.procurement.dossier.application.model.data.submission.finalize.FinalizeSubmissionsResult
import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsParams
import com.procurement.dossier.application.model.data.submission.find.FindSubmissionsResult
import com.procurement.dossier.application.model.data.submission.get.GetInvitedCandidatesOwnersParams
import com.procurement.dossier.application.model.data.submission.get.GetInvitedCandidatesOwnersResult
import com.procurement.dossier.application.model.data.submission.get.GetSubmissionsByQualificationIdsParams
import com.procurement.dossier.application.model.data.submission.get.GetSubmissionsByQualificationIdsResult
import com.procurement.dossier.application.model.data.submission.get.fromDomain
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
import com.procurement.dossier.domain.model.enums.PartyRole
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
import com.procurement.dossier.infrastructure.model.dto.response.PersonesProcessingResponse
import org.springframework.stereotype.Service

@Service
class SubmissionService(
    private val submissionRepository: SubmissionRepository,
    private val rulesRepository: RulesRepository,
    private val generable: Generable
) {
    fun createSubmission(params: CreateSubmissionParams): Result<CreateSubmissionResult, Fail.Incident> {
        val receivedCandidates = params.submission.candidates.toSetBy { it.id }

        val submissionsToWithdraw = submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)
            .orForwardFail { return it }
            .asSequence()
            .filter { submission -> submission.status == SubmissionStatus.PENDING }
            .filter { submission -> isSubmittedByReceivedCandidates(submission, receivedCandidates) }
            .map { submission -> submission.copy(status = SubmissionStatus.WITHDRAWN) }
            .toList()

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
                    value = requirementResponse.value,
                    evidences = requirementResponse.evidences
                        .map { evidence ->
                            Submission.RequirementResponse.Evidence(
                                id = evidence.id,
                                description = evidence.description,
                                title = evidence.title,
                                relatedDocument = evidence.relatedDocument
                                    ?.let { relatedDocument ->
                                        Submission.RequirementResponse.Evidence.RelatedDocument(
                                            id = relatedDocument.id
                                        )
                                    }
                            )
                        }
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

    fun checkAccessToSubmission(params: CheckAccessToSubmissionParams): ValidationResult<Fail>  {
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

    fun getInvitedCandidatesOwners(params: GetInvitedCandidatesOwnersParams): Result<GetInvitedCandidatesOwnersResult, Fail> {
        val submissions = submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)
            .orForwardFail { return it }

        if (submissions.isEmpty())
            return GetInvitedCandidatesOwnersErrors.SubmissionNotFound(cpid = params.cpid, ocid = params.ocid).asFailure()

        return submissions.asSequence()
            .filter { submission -> submission.status == SubmissionStatus.VALID }
            .map { submission -> GetInvitedCandidatesOwnersResult.Candidate.fromDomain(submission) }
            .let { GetInvitedCandidatesOwnersResult(it.toList()) }
            .asSuccess()
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
                value = requirementResponse.value,
                evidences = requirementResponse.evidences
                    .map { evidence ->
                        FindSubmissionsResult.RequirementResponse.Evidence(
                            id = evidence.id,
                            description = evidence.description,
                            title = evidence.title,
                            relatedDocument = evidence.relatedDocument
                                ?.let { relatedDocument ->
                                    FindSubmissionsResult.RequirementResponse.Evidence.RelatedDocument(
                                        id = relatedDocument.id
                                    )
                                }
                        )
                    }
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

        checkDuplicatesWithinPerson(params)
            .doOnError { return ValidationResult.error(it) }

        val responsesByCandidates = params.requirementResponses
            .groupBy { requirementResponse -> requirementResponse.relatedCandidate.id }

        checkDuplicatesWithinResponses(responsesByCandidates)
            .doOnError { return ValidationResult.error(it) }

        checkEvidenceDocuments(params)
            .doOnError { return ValidationResult.error(it) }

        checkCandidateScheme(params)
            .doOnError { return ValidationResult.error(it) }

        return ValidationResult.ok()
    }

    private fun checkCandidateScheme(params: ValidateSubmissionParams): ValidationResult<ValidationErrors.SchemeNotFound> {
        val registrationSchemesByCountry = params.mdm.registrationSchemes.associateBy(
            keySelector = { it.country },
            valueTransform = { it.schemes.toSet() }
        )

        params.candidates.forEach { candidate ->
            val candidateCountry = candidate.address.addressDetails.country.id
            val registrationSchemes = registrationSchemesByCountry[candidateCountry].orEmpty()
            val identifierScheme = candidate.identifier.scheme
            if (identifierScheme !in registrationSchemes)
                return ValidationResult.error(
                    ValidationErrors.SchemeNotFound(
                        identifierScheme = identifierScheme,
                        country = candidateCountry,
                        candidateId = candidate.id
                    )
                )

        }
        return ValidationResult.ok()
    }

    private fun checkEvidenceDocuments(params: ValidateSubmissionParams): ValidationResult<ValidationErrors.EvidenceDocumentMissing> {
        val evidenceDocuments = params.requirementResponses
            .flatMap { requirementResponse -> requirementResponse.evidences }
            .mapNotNull { it.relatedDocument }
            .map { it.id }

        val documents = params.documents.map { it.id }
        val missingDocuments = evidenceDocuments - documents
        return if (missingDocuments.isNotEmpty())
            ValidationResult.error(ValidationErrors.EvidenceDocumentMissing(missingDocuments))
        else ValidationResult.ok()
    }

    private fun checkDuplicatesWithinResponses(responsesByCandidates: Map<String, List<ValidateSubmissionParams.RequirementResponse>>)
        : ValidationResult<ValidationErrors.DuplicateRequirementResponseByOrganization> {
        val duplicateResponseWithCandidate = responsesByCandidates
            .asSequence()
            .mapNotNull { entry ->
                val candidate = entry.key
                val responses = entry.value

                responses.getDuplicate { it.requirement.id }
                    ?.let { duplicateResponse -> Pair(duplicateResponse, candidate) }
            }
            .firstOrNull()
        return if (duplicateResponseWithCandidate != null)
            ValidationResult.error(
                ValidationErrors.DuplicateRequirementResponseByOrganization(
                    requirementId = duplicateResponseWithCandidate.first.requirement.id,
                    candidateId = duplicateResponseWithCandidate.second
                )
            )
        else ValidationResult.ok()
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
                                value = requirementResponse.value,
                                evidences = requirementResponse.evidences.map { evidence ->
                                    GetSubmissionsByQualificationIdsResult.Submissions.Detail.RequirementResponse.Evidence(
                                        id = evidence.id,
                                        title = evidence.title,
                                        description = evidence.description,
                                        relatedDocument = evidence.relatedDocument?.let { relatedDocument ->
                                            GetSubmissionsByQualificationIdsResult.Submissions.Detail.RequirementResponse.Evidence.RelatedDocument(
                                                id = relatedDocument.id
                                            )
                                        }
                                    )
                                }
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

    fun checkPresenceCandidateInOneSubmission(params: CheckPresenceCandidateInOneSubmissionParams): ValidationResult<Fail> {
        val duplicateCandidate = params.submissions.details.flatMap { it.candidates }.getDuplicate { it.id }
        return if (duplicateCandidate != null)
            ValidationResult.error(
                ValidationErrors.CheckPresenceCandidateInOneSubmission.DuplicateCandidate(
                    duplicateCandidate.id
                )
            ) else ValidationResult.ok()
    }

    fun personesProcessing(params: PersonesProcessingParams): Result<PersonesProcessingResponse, Fail> {
        when(params.role){
            PartyRole.INVITED_CANDIDATE -> {
                val submissions = submissionRepository.findBy(cpid = params.cpid, ocid = params.ocid)
                    .orForwardFail { fail -> return fail }
                    .filter { it.status == SubmissionStatus.VALID }

                if (submissions.isEmpty())
                    return ValidationErrors.PersonesProcessing.ValidSubmissionNotFound(params.cpid, params.ocid)
                        .asFailure()

                val receivedParty = params.parties.first()
                val submission = submissions.firstOrNull {
                    it.candidates.map { it.id }.contains(receivedParty.id)
                } ?: return ValidationErrors.PersonesProcessing.CandidateNotFound(receivedParty.id)
                    .asFailure()

                val candidateToUpdate = submission.candidates.firstOrNull { it.id == receivedParty.id }!!
                val updatedPersones = getUpdatedPersones(receivedParty, candidateToUpdate)
                val updatedCandidate = candidateToUpdate.copy(persones = updatedPersones)
                val updatedCandidates = submission.candidates.map { candidate ->
                    if (candidate.id == candidateToUpdate.id)
                        updatedCandidate
                    else candidate
                }
                val updatedSubmission = submission.copy(candidates = updatedCandidates)
                submissionRepository.saveSubmission(params.cpid, params.ocid, updatedSubmission)
                    .doOnFail { return it.asFailure() }

                return updatedCandidate.toPersonesProcessingResponse().asSuccess()
            }
            PartyRole.BUYER,
            PartyRole.PROCURING_ENTITY,
            PartyRole.CLIENT,
            PartyRole.CENTRAL_PURCHASING_BODY,
            PartyRole.AUTHOR,
            PartyRole.CANDIDATE,
            PartyRole.ENQUIRER,
            PartyRole.FUNDER,
            PartyRole.INVITED_TENDERER,
            PartyRole.PAYEE,
            PartyRole.PAYER,
            PartyRole.REVIEW_BODY,
            PartyRole.SUPPLIER,
            PartyRole.TENDERER -> throw ErrorException(ErrorType.INVALID_ROLE)
        }
    }

    private fun getUpdatedPersones(
        receivedParty: PersonesProcessingParams.Party,
        candidate: Submission.Candidate
    ): List<Submission.Candidate.Person> =
        updateStrategy(
            receivedElements = receivedParty.persones,
            keyExtractorForReceivedElement = { it.id.value },
            availableElements = candidate.persones,
            keyExtractorForAvailableElement = { it.id },
            updateBlock = { received -> this.updateBy(received) },
            createBlock = { received -> received.toDomain() }
        )

    private fun Submission.Candidate.Person.updateBy(
        receivedPerson: PersonesProcessingParams.Party.Persone
    ): Submission.Candidate.Person {
        val updatedBusinessFunctions = updateStrategy(
            receivedElements = receivedPerson.businessFunctions,
            keyExtractorForReceivedElement = { it.id },
            availableElements = businessFunctions,
            keyExtractorForAvailableElement = { it.id },
            updateBlock = { received -> this.updateBy(received) },
            createBlock = { received -> received.toDomain() }
        )

        return this.copy(
            title = receivedPerson.title,
            name = receivedPerson.name,
            identifier = receivedPerson.identifier.let { identifier ->
                Submission.Candidate.Person.Identifier(
                    id = identifier.id,
                    scheme = identifier.scheme,
                    uri = identifier.uri ?: this.identifier.uri
                )
            },
            businessFunctions = updatedBusinessFunctions
        )
    }

    private fun Submission.Candidate.Person.BusinessFunction.updateBy(receivedBusinessFunction: PersonesProcessingParams.Party.Persone.BusinessFunction): Submission.Candidate.Person.BusinessFunction {
        val updatedDocuments = updateStrategy(
            receivedElements = receivedBusinessFunction.documents.orEmpty(),
            keyExtractorForReceivedElement = { it.id },
            availableElements = documents,
            keyExtractorForAvailableElement = { it.id },
            updateBlock = { received -> this.updateBy(received) },
            createBlock = { received -> received.toDomain() }
        )

        return this.copy(
            type = receivedBusinessFunction.type,
            jobTitle = receivedBusinessFunction.jobTitle,
            period = receivedBusinessFunction.period.let {
                Submission.Candidate.Person.BusinessFunction.Period(it.startDate)
            },
            documents = updatedDocuments
        )
    }

    private fun Submission.Candidate.Person.BusinessFunction.Document.updateBy(receivedDocument: PersonesProcessingParams.Party.Persone.BusinessFunction.Document) = this.copy(
        documentType = receivedDocument.documentType,
        description = receivedDocument.description ?: this.description,
        title = receivedDocument.title
    )

    private fun PersonesProcessingParams.Party.Persone.toDomain() =
        Submission.Candidate.Person(
            id = id.value,
            name = name,
            title = title,
            identifier = Submission.Candidate.Person.Identifier(
                id = identifier.id,
                scheme = identifier.scheme,
                uri = identifier.uri
            ),
            businessFunctions = businessFunctions.map { businessFunction -> businessFunction.toDomain() }
        )

    private fun PersonesProcessingParams.Party.Persone.BusinessFunction.Document.toDomain() =
        Submission.Candidate.Person.BusinessFunction.Document(
            id = id,
            title = title,
            documentType = documentType,
            description = description
        )

    private fun PersonesProcessingParams.Party.Persone.BusinessFunction.toDomain() =
        Submission.Candidate.Person.BusinessFunction(
            id = id,
            period = period.let { Submission.Candidate.Person.BusinessFunction.Period(startDate = it.startDate) },
            jobTitle = jobTitle,
            type = type,
            documents = documents?.map { document ->
                document.toDomain()
            }.orEmpty()
        )

    private fun  Submission.Candidate.toPersonesProcessingResponse() = PersonesProcessingResponse(
        parties = listOf(
            PersonesProcessingResponse.Party(
                id = id,
                name = name,
                identifier = identifier
                    .let { identifier ->
                        PersonesProcessingResponse.Party.Identifier(
                            scheme = identifier.scheme,
                            id = identifier.id,
                            legalName = identifier.legalName,
                            uri = identifier.uri
                        )
                    },
                additionalIdentifiers = additionalIdentifiers
                    .map { additionalIdentifier ->
                        PersonesProcessingResponse.Party.AdditionalIdentifier(
                            scheme = additionalIdentifier.scheme,
                            id = additionalIdentifier.id,
                            legalName = additionalIdentifier.legalName,
                            uri = additionalIdentifier.uri
                        )
                    },
                address = address
                    .let { address ->
                        PersonesProcessingResponse.Party.Address(
                            streetAddress = address.streetAddress,
                            postalCode = address.postalCode,
                            addressDetails = address.addressDetails
                                .let { addressDetails ->
                                    PersonesProcessingResponse.Party.Address.AddressDetails(
                                        country = addressDetails.country
                                            .let { country ->
                                                PersonesProcessingResponse.Party.Address.AddressDetails.Country(
                                                    scheme = country.scheme,
                                                    id = country.id,
                                                    description = country.description,
                                                    uri = country.uri
                                                )
                                            },
                                        region = addressDetails.region
                                            .let { region ->
                                                PersonesProcessingResponse.Party.Address.AddressDetails.Region(
                                                    scheme = region.scheme,
                                                    id = region.id,
                                                    description = region.description,
                                                    uri = region.uri
                                                )
                                            },
                                        locality = addressDetails.locality
                                            .let { locality ->
                                                PersonesProcessingResponse.Party.Address.AddressDetails.Locality(
                                                    scheme = locality.scheme,
                                                    id = locality.id,
                                                    description = locality.description,
                                                    uri = locality.uri
                                                )
                                            }
                                    )
                                }
                        )
                    },
                contactPoint = contactPoint
                    .let { contactPoint ->
                        PersonesProcessingResponse.Party.ContactPoint(
                            name = contactPoint.name,
                            email = contactPoint.email,
                            telephone = contactPoint.telephone,
                            faxNumber = contactPoint.faxNumber,
                            url = contactPoint.url
                        )
                    },
                persones = persones.map { person ->
                    PersonesProcessingResponse.Party.Persone(
                        id = person.id,
                        title = person.title,
                        name = person.name,
                        identifier = person.identifier
                            .let { identifier ->
                                PersonesProcessingResponse.Party.Persone.Identifier(
                                    id = identifier.id,
                                    scheme = identifier.scheme,
                                    uri = identifier.uri
                                )
                            },
                        businessFunctions = person.businessFunctions
                            .map { businessFunctions ->
                                PersonesProcessingResponse.Party.Persone.BusinessFunction(
                                    id = businessFunctions.id,
                                    jobTitle = businessFunctions.jobTitle,
                                    type = businessFunctions.type,
                                    period = businessFunctions.period
                                        .let { period ->
                                            PersonesProcessingResponse.Party.Persone.BusinessFunction.Period(
                                                startDate = period.startDate
                                            )
                                        },
                                    documents = businessFunctions.documents
                                        .map { document ->
                                            PersonesProcessingResponse.Party.Persone.BusinessFunction.Document(
                                                id = document.id,
                                                title = document.title,
                                                description = document.description,
                                                documentType = document.documentType
                                            )
                                        }
                                )
                            }
                    )
                }
            ))
    )

}