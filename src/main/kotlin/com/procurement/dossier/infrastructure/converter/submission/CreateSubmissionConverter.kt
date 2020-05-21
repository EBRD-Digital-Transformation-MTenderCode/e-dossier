package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionParams
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionResult
import com.procurement.dossier.domain.util.extension.mapResult
import com.procurement.dossier.infrastructure.model.dto.request.submission.CreateSubmissionRequest
import com.procurement.dossier.infrastructure.model.submission.Submission

fun CreateSubmissionRequest.convert() =
    CreateSubmissionParams.tryCreate(
        cpid = cpid,
        date = date,
        ocid = ocid,
        owner = owner,
        submission = submission.convert().orForwardFail { fail -> return fail }
    )

private fun CreateSubmissionRequest.Submission.convert() =
    CreateSubmissionParams.Submission.tryCreate(
        documents = documents?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        candidates = candidates.mapResult { it.convert() }.orForwardFail { fail -> return fail },
        requirementResponses = requirementResponses?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        id = id
    )

private fun CreateSubmissionRequest.Submission.Document.convert() =
    CreateSubmissionParams.Submission.Document.tryCreate(
        id = id, documentType = documentType, description = description, title = title
    )

private fun CreateSubmissionRequest.Submission.Candidate.convert() =
    CreateSubmissionParams.Submission.Candidate.tryCreate(
        id = id,
        details = details.convert().orForwardFail { fail -> return fail },
        persones = persones?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        contactPoint = contactPoint.let { contactPoint ->
            CreateSubmissionParams.Submission.Candidate.ContactPoint(
                name = contactPoint.name,
                email = contactPoint.email,
                faxNumber = contactPoint.faxNumber,
                telephone = contactPoint.telephone,
                url = contactPoint.url
            )
        },
        address = address.let { address ->
            CreateSubmissionParams.Submission.Candidate.Address(
                streetAddress = address.streetAddress,
                postalCode = address.postalCode,
                addressDetails = address.addressDetails.let { addressDetails ->
                    CreateSubmissionParams.Submission.Candidate.Address.AddressDetails(
                        country = addressDetails.country.let { country ->
                            CreateSubmissionParams.Submission.Candidate.Address.AddressDetails.Country(
                                id = country.id,
                                scheme = country.scheme,
                                description = country.description
                            )
                        },
                        locality = addressDetails.locality.let { locality ->
                            CreateSubmissionParams.Submission.Candidate.Address.AddressDetails.Locality(
                                id = locality.id,
                                scheme = locality.scheme,
                                description = locality.description
                            )
                        },
                        region = addressDetails.region.let { region ->
                            CreateSubmissionParams.Submission.Candidate.Address.AddressDetails.Region(
                                id = region.id,
                                scheme = region.scheme,
                                description = region.description
                            )
                        }
                    )
                }
            )
        },
        additionalIdentifier = additionalIdentifiers?.map { additionalIdentifier ->
            CreateSubmissionParams.Submission.Candidate.AdditionalIdentifier(
                id = additionalIdentifier.id,
                uri = additionalIdentifier.uri,
                scheme = additionalIdentifier.scheme,
                legalName = additionalIdentifier.legalName
            )
        },
        identifier = identifier.let { identifier ->
            CreateSubmissionParams.Submission.Candidate.Identifier(
                id = identifier.id,
                legalName = identifier.legalName,
                scheme = identifier.scheme,
                uri = identifier.uri
            )
        },
        name = name
    )

private fun CreateSubmissionRequest.Submission.Candidate.Details.convert() =
    CreateSubmissionParams.Submission.Candidate.Details.tryCreate(
        typeOfSupplier = typeOfSupplier,
        bankAccounts = bankAccounts?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        legalForm = legalForm?.let { legalForm ->
            CreateSubmissionParams.Submission.Candidate.Details.LegalForm(
                scheme = legalForm.scheme,
                description = legalForm.description,
                id = legalForm.id,
                uri = legalForm.uri
            )
        },
        mainEconomicActivity = mainEconomicActivities?.map { mainEconomicActivity ->
            CreateSubmissionParams.Submission.Candidate.Details.MainEconomicActivity(
                scheme = mainEconomicActivity.scheme,
                uri = mainEconomicActivity.uri,
                id = mainEconomicActivity.id,
                description = mainEconomicActivity.description
            )
        },
        scale = scale
    )

private fun CreateSubmissionRequest.Submission.Candidate.Details.BankAccount.convert() =
    CreateSubmissionParams.Submission.Candidate.Details.BankAccount.tryCreate(
        description = description,
        identifier = identifier.let { identifier ->
            CreateSubmissionParams.Submission.Candidate.Details.BankAccount.Identifier(
                id = identifier.id,
                scheme = identifier.scheme
            )
        },
        address = address.let { address ->
            CreateSubmissionParams.Submission.Candidate.Details.BankAccount.Address(
                streetAddress = address.streetAddress,
                postalCode = address.postalCode,
                addressDetails = address.addressDetails.let { addressDetails ->
                    CreateSubmissionParams.Submission.Candidate.Details.BankAccount.Address.AddressDetails(
                        country = addressDetails.country.let { country ->
                            CreateSubmissionParams.Submission.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                id = country.id,
                                scheme = country.scheme,
                                description = country.description
                            )
                        },
                        locality = addressDetails.locality.let { locality ->
                            CreateSubmissionParams.Submission.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                id = locality.id,
                                scheme = locality.scheme,
                                description = locality.description
                            )
                        },
                        region = addressDetails.region.let { region ->
                            CreateSubmissionParams.Submission.Candidate.Details.BankAccount.Address.AddressDetails.Region(
                                id = region.id,
                                scheme = region.scheme,
                                description = region.description
                            )
                        }
                    )
                }
            )
        },
        accountIdentification = accountIdentification.let { accountIdentification ->
            CreateSubmissionParams.Submission.Candidate.Details.BankAccount.AccountIdentification(
                id = accountIdentification.id,
                scheme = accountIdentification.scheme
            )
        },
        additionalAccountIdentifiers = additionalAccountIdentifiers?.map { additionalAccountIdentifiers ->
            CreateSubmissionParams.Submission.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                scheme = additionalAccountIdentifiers.scheme,
                id = additionalAccountIdentifiers.id
            )
        },
        bankName = bankName
    )

private fun CreateSubmissionRequest.Submission.Candidate.Person.convert() =
    CreateSubmissionParams.Submission.Candidate.Person.tryCreate(
        title = title,
        identifier = identifier.let { identifier ->
            CreateSubmissionParams.Submission.Candidate.Person.Identifier(
                scheme = identifier.scheme,
                id = identifier.id,
                uri = identifier.uri
            )
        },
        name = name,
        businessFunctions = businessFunctions.mapResult { it.convert() }.orForwardFail { fail -> return fail }
    )

private fun CreateSubmissionRequest.Submission.Candidate.Person.BusinessFunction.convert() =
    CreateSubmissionParams.Submission.Candidate.Person.BusinessFunction.tryCreate(
        id = id,
        documents = documents?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        period = period.convert().orForwardFail { fail -> return fail },
        jobTitle = jobTitle,
        type = type
    )

private fun CreateSubmissionRequest.Submission.Candidate.Person.BusinessFunction.Document.convert() =
    CreateSubmissionParams.Submission.Candidate.Person.BusinessFunction.Document.tryCreate(
        id = id, description = description, title = title, documentType = documentType
    )

private fun CreateSubmissionRequest.Submission.Candidate.Person.BusinessFunction.Period.convert() =
    CreateSubmissionParams.Submission.Candidate.Person.BusinessFunction.Period.tryCreate(startDate = startDate)

private fun CreateSubmissionRequest.Submission.RequirementResponse.convert() =
    CreateSubmissionParams.Submission.RequirementResponse.tryCreate(
        id = id,
        value = value,
        requirement = requirement.let { requirement ->
            CreateSubmissionParams.Submission.RequirementResponse.Requirement(
                id = requirement.id
            )
        },
        relatedCandidate = relatedCandidate.let { relatedCandidate ->
            CreateSubmissionParams.Submission.RequirementResponse.RelatedCandidate(
                id = relatedCandidate.id,
                name = relatedCandidate.name
            )
        }
    )

fun Submission.toCreateSubmissionResult() =
    CreateSubmissionResult(
        id = id,
        token = token,
        date = date,
        status = status,
        documents = documents.map { document ->
            CreateSubmissionResult.Document(
                id = document.id,
                description = document.description,
                documentType = document.documentType,
                title = document.title
            )
        },
        candidates = candidates.map { candidate ->
            CreateSubmissionResult.Candidate(
                id = candidate.id,
                name = candidate.name
            )
        },
        requirementResponses = requirementResponses.map { requirementResponse ->
            CreateSubmissionResult.RequirementResponse(
                id = requirementResponse.id,
                relatedCandidate = requirementResponse.relatedCandidate.let { relatedCandidate ->
                    CreateSubmissionResult.RequirementResponse.RelatedCandidate(
                        id = relatedCandidate.id,
                        name = relatedCandidate.name
                    )
                },
                requirement = requirementResponse.requirement.let { requirement ->
                    CreateSubmissionResult.RequirementResponse.Requirement(
                        id = requirement.id
                    )
                },
                value = requirementResponse.value
            )
        }
    )