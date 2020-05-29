package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.validate.ValidateSubmissionParams
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.extension.mapResult
import com.procurement.dossier.infrastructure.model.dto.request.submission.ValidateSubmissionRequest

fun ValidateSubmissionRequest.convert(): Result<ValidateSubmissionParams, DataErrors> =
    ValidateSubmissionParams.tryCreate(
        documents = documents?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        candidates = candidates.mapResult { it.convert() }.orForwardFail { fail -> return fail },
        id = id
    )

private fun ValidateSubmissionRequest.Document.convert() =
    ValidateSubmissionParams.Document.tryCreate(
        id = id, documentType = documentType, description = description, title = title
    )

private fun ValidateSubmissionRequest.Candidate.convert() =
    ValidateSubmissionParams.Candidate.tryCreate(
        id = id,
        details = details.convert().orForwardFail { fail -> return fail },
        persones = persones?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        contactPoint = contactPoint.let { contactPoint ->
            ValidateSubmissionParams.Candidate.ContactPoint(
                name = contactPoint.name,
                email = contactPoint.email,
                faxNumber = contactPoint.faxNumber,
                telephone = contactPoint.telephone,
                url = contactPoint.url
            )
        },
        address = address.let { address ->
            ValidateSubmissionParams.Candidate.Address(
                streetAddress = address.streetAddress,
                postalCode = address.postalCode,
                addressDetails = address.addressDetails.let { addressDetails ->
                    ValidateSubmissionParams.Candidate.Address.AddressDetails(
                        country = addressDetails.country.let { country ->
                            ValidateSubmissionParams.Candidate.Address.AddressDetails.Country(
                                id = country.id,
                                scheme = country.scheme,
                                description = country.description
                            )
                        },
                        locality = addressDetails.locality.let { locality ->
                            ValidateSubmissionParams.Candidate.Address.AddressDetails.Locality(
                                id = locality.id,
                                scheme = locality.scheme,
                                description = locality.description
                            )
                        },
                        region = addressDetails.region.let { region ->
                            ValidateSubmissionParams.Candidate.Address.AddressDetails.Region(
                                id = region.id,
                                scheme = region.scheme,
                                description = region.description
                            )
                        }
                    )
                }
            )
        },
        additionalIdentifiers = additionalIdentifiers?.map { additionalIdentifier ->
            ValidateSubmissionParams.Candidate.AdditionalIdentifier(
                id = additionalIdentifier.id,
                uri = additionalIdentifier.uri,
                scheme = additionalIdentifier.scheme,
                legalName = additionalIdentifier.legalName
            )
        },
        identifier = identifier.let { identifier ->
            ValidateSubmissionParams.Candidate.Identifier(
                id = identifier.id,
                legalName = identifier.legalName,
                scheme = identifier.scheme,
                uri = identifier.uri
            )
        },
        name = name
    )

private fun ValidateSubmissionRequest.Candidate.Details.convert() =
    ValidateSubmissionParams.Candidate.Details.tryCreate(
        typeOfSupplier = typeOfSupplier,
        bankAccounts = bankAccounts?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        legalForm = legalForm?.let { legalForm ->
            ValidateSubmissionParams.Candidate.Details.LegalForm(
                scheme = legalForm.scheme,
                description = legalForm.description,
                id = legalForm.id,
                uri = legalForm.uri
            )
        },
        mainEconomicActivities = mainEconomicActivities?.map { mainEconomicActivity ->
            ValidateSubmissionParams.Candidate.Details.MainEconomicActivity(
                scheme = mainEconomicActivity.scheme,
                uri = mainEconomicActivity.uri,
                id = mainEconomicActivity.id,
                description = mainEconomicActivity.description
            )
        },
        scale = scale
    )

private fun ValidateSubmissionRequest.Candidate.Details.BankAccount.convert() =
    ValidateSubmissionParams.Candidate.Details.BankAccount.tryCreate(
        description = description,
        identifier = identifier.let { identifier ->
            ValidateSubmissionParams.Candidate.Details.BankAccount.Identifier(
                id = identifier.id,
                scheme = identifier.scheme
            )
        },
        address = address.let { address ->
            ValidateSubmissionParams.Candidate.Details.BankAccount.Address(
                streetAddress = address.streetAddress,
                postalCode = address.postalCode,
                addressDetails = address.addressDetails.let { addressDetails ->
                    ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails(
                        country = addressDetails.country.let { country ->
                            ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                id = country.id,
                                scheme = country.scheme,
                                description = country.description
                            )
                        },
                        locality = addressDetails.locality.let { locality ->
                            ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                id = locality.id,
                                scheme = locality.scheme,
                                description = locality.description
                            )
                        },
                        region = addressDetails.region.let { region ->
                            ValidateSubmissionParams.Candidate.Details.BankAccount.Address.AddressDetails.Region(
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
            ValidateSubmissionParams.Candidate.Details.BankAccount.AccountIdentification(
                id = accountIdentification.id,
                scheme = accountIdentification.scheme
            )
        },
        additionalAccountIdentifiers = additionalAccountIdentifiers?.map { additionalAccountIdentifiers ->
            ValidateSubmissionParams.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                scheme = additionalAccountIdentifiers.scheme,
                id = additionalAccountIdentifiers.id
            )
        },
        bankName = bankName
    )

private fun ValidateSubmissionRequest.Candidate.Person.convert() =
    ValidateSubmissionParams.Candidate.Person.tryCreate(
        id = id,
        title = title,
        identifier = identifier.let { identifier ->
            ValidateSubmissionParams.Candidate.Person.Identifier(
                scheme = identifier.scheme,
                id = identifier.id,
                uri = identifier.uri
            )
        },
        name = name,
        businessFunctions = businessFunctions.mapResult { it.convert() }.orForwardFail { fail -> return fail }
    )

private fun ValidateSubmissionRequest.Candidate.Person.BusinessFunction.convert() =
    ValidateSubmissionParams.Candidate.Person.BusinessFunction.tryCreate(
        id = id,
        documents = documents?.mapResult { it.convert() }?.orForwardFail { fail -> return fail },
        period = period.convert().orForwardFail { fail -> return fail },
        jobTitle = jobTitle,
        type = type
    )

private fun ValidateSubmissionRequest.Candidate.Person.BusinessFunction.Document.convert() =
    ValidateSubmissionParams.Candidate.Person.BusinessFunction.Document.tryCreate(
        id = id, description = description, title = title, documentType = documentType
    )

private fun ValidateSubmissionRequest.Candidate.Person.BusinessFunction.Period.convert() =
    ValidateSubmissionParams.Candidate.Person.BusinessFunction.Period.tryCreate(startDate = startDate)