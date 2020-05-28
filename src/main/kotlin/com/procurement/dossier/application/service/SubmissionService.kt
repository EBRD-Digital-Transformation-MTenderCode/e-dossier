package com.procurement.dossier.application.service

import com.procurement.dossier.application.model.data.submission.check.CheckAccessToSubmissionParams
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionParams
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionResult
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsParams
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsResult
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionParams
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionResult
import com.procurement.dossier.application.repository.SubmissionRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.doOnFalse
import com.procurement.dossier.domain.util.extension.getUnknownElements
import com.procurement.dossier.infrastructure.converter.submission.toCreateSubmissionResult
import org.springframework.stereotype.Service

@Service
class SubmissionService(
    private val submissionRepository: SubmissionRepository,
    private val generable: Generable
) {
    fun createSubmission(params: CreateSubmissionParams): Result<CreateSubmissionResult, Fail.Incident> {
        val submission = params.convert()
        submissionRepository.saveSubmission(cpid = params.cpid, ocid = params.ocid, submission = submission)
            .doOnFail { incident -> return incident.asFailure() }
        return submission.toCreateSubmissionResult().asSuccess()
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
    ): ValidationResult<ValidationErrors.SubmissionNotFoundFor> {
        val unknownElements = known.getUnknownElements(received = received)
        return if (unknownElements.isNotEmpty())
            ValidationResult.error(ValidationErrors.SubmissionNotFoundFor.GetSubmissionStateByIds(unknownElements.joinToString()))
        else ValidationResult.ok()
    }

    fun setStateForSubmission(params: SetStateForSubmissionParams): Result<SetStateForSubmissionResult, Fail> {
        val submission = params.submission

        submissionRepository.setSubmissionStatus(
            cpid = params.cpid, ocid = params.ocid, id = submission.id, status = submission.status
        ).orForwardFail { fail -> return fail }
            .doOnFalse {
                return ValidationErrors.SubmissionNotFoundFor.SetStateForSubmission(id = submission.id.toString())
                    .asFailure()
            }
        return SetStateForSubmissionResult(id = submission.id, status = submission.status).asSuccess()
    }

    fun checkAccessToSubmission(params: CheckAccessToSubmissionParams): ValidationResult<Fail> {
        val credentials = submissionRepository.getSubmissionCredentials(
            cpid = params.cpid, ocid = params.ocid, id = params.submissionId
        ).doReturn { incident -> return ValidationResult.error(incident) }
            ?: return ValidationResult.error(ValidationErrors.SubmissionNotFoundFor.CheckAccessToSubmission(id = params.submissionId.toString()))

        if (params.token != credentials.token)
            return ValidationResult.error(ValidationErrors.InvalidToken())

        if (params.owner != credentials.owner)
            return ValidationResult.error(ValidationErrors.InvalidOwner())

        return ValidationResult.ok()
    }
}