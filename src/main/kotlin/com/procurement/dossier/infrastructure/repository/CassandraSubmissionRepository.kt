package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.SubmissionRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.extension.cassandra.tryExecute
import com.procurement.dossier.infrastructure.model.entity.submission.SubmissionDataEntity
import com.procurement.dossier.infrastructure.model.submission.Submission
import com.procurement.dossier.infrastructure.utils.tryToJson
import org.springframework.stereotype.Repository

@Repository
class CassandraSubmissionRepository(private val session: Session) : SubmissionRepository {
    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "submission"
        private const val columnCpid = "cpid"
        private const val columnOcid = "ocid"
        private const val columnId = "id"
        private const val columnStatus = "status"
        private const val columnToken = "token"
        private const val columnOwner = "owner"
        private const val columnJsonData = "json_data"

        private const val SAVE_SUBMISSION_CQL = """
               INSERT INTO ${keySpace}.${tableName}(
                      ${columnCpid},
                      ${columnOcid},
                      ${columnId},
                      ${columnStatus},
                      ${columnToken},
                      ${columnOwner},
                      ${columnJsonData}
               )
               VALUES(?, ?, ?, ?, ?, ?, ?)                
            """
    }

    private val preparedSaveSubmissionCQL = session.prepare(SAVE_SUBMISSION_CQL)

    override fun saveSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): ValidationResult<Fail.Incident> {
        val entity = submission.convert()
        val jsonData = tryToJson(entity).doReturn { incident -> return ValidationResult.error(incident) }
        val statement = preparedSaveSubmissionCQL.bind()
            .apply {
                setString(columnCpid, cpid.toString())
                setString(columnOcid, ocid.toString())
                setString(columnStatus, submission.status.key)
                setUUID(columnToken, submission.token)
                setUUID(columnOwner, submission.owner)
                setString(columnJsonData, jsonData)
            }

        statement.tryExecute(session).doReturn { fail -> return ValidationResult.error(fail) }
        return ValidationResult.ok()
    }

    private fun Submission.convert() =
        SubmissionDataEntity(
            id = id,
            date = date,
            requirementResponses = requirementResponses.map { requirementResponse ->
                SubmissionDataEntity.RequirementResponse(
                    id = requirementResponse.id,
                    relatedCandidate = requirementResponse.relatedCandidate.let { relatedCandidate ->
                        SubmissionDataEntity.RequirementResponse.RelatedCandidate(
                            id = relatedCandidate.id,
                            name = relatedCandidate.name
                        )
                    },
                    requirement = requirementResponse.requirement.let { requirement ->
                        SubmissionDataEntity.RequirementResponse.Requirement(
                            id = requirement.id
                        )
                    },
                    value = requirementResponse.value
                )
            },
            documents = documents.map { document ->
                SubmissionDataEntity.Document(
                    id = document.id,
                    description = document.description,
                    documentType = document.documentType,
                    title = document.title
                )
            },
            candidates = candidates.map { candidate ->
                SubmissionDataEntity.Candidate(
                    id = candidate.id,
                    name = candidate.name,
                    additionalIdentifiers = candidate.additionalIdentifiers.map { additionalIdentifier ->
                        SubmissionDataEntity.Candidate.AdditionalIdentifier(
                            id = additionalIdentifier.id,
                            legalName = additionalIdentifier.legalName,
                            scheme = additionalIdentifier.scheme,
                            uri = additionalIdentifier.uri
                        )
                    },
                    address = candidate.address.let { address ->
                        SubmissionDataEntity.Candidate.Address(
                            streetAddress = address.streetAddress,
                            postalCode = address.postalCode,
                            addressDetails = address.addressDetails.let { addressDetails ->
                                SubmissionDataEntity.Candidate.Address.AddressDetails(
                                    country = addressDetails.country.let { country ->
                                        SubmissionDataEntity.Candidate.Address.AddressDetails.Country(
                                            id = country.id,
                                            scheme = country.scheme,
                                            description = country.description
                                        )
                                    },
                                    locality = addressDetails.locality.let { locality ->
                                        SubmissionDataEntity.Candidate.Address.AddressDetails.Locality(
                                            id = locality.id,
                                            scheme = locality.scheme,
                                            description = locality.description
                                        )
                                    },
                                    region = addressDetails.region.let { region ->
                                        SubmissionDataEntity.Candidate.Address.AddressDetails.Region(
                                            id = region.id,
                                            scheme = region.scheme,
                                            description = region.description
                                        )
                                    }
                                )
                            }
                        )

                    },
                    contactPoint = candidate.contactPoint.let { contactPoint ->
                        SubmissionDataEntity.Candidate.ContactPoint(
                            name = contactPoint.name,
                            email = contactPoint.email,
                            faxNumber = contactPoint.faxNumber,
                            telephone = contactPoint.telephone,
                            url = contactPoint.url
                        )
                    },
                    details = candidate.details.let { details ->
                        SubmissionDataEntity.Candidate.Details(
                            typeOfSupplier = details.typeOfSupplier,
                            bankAccounts = details.bankAccounts.map { bankAccount ->
                                SubmissionDataEntity.Candidate.Details.BankAccount(
                                    description = bankAccount.description,
                                    address = bankAccount.address.let { address ->
                                        SubmissionDataEntity.Candidate.Details.BankAccount.Address(
                                            streetAddress = address.streetAddress,
                                            postalCode = address.postalCode,
                                            addressDetails = address.addressDetails.let { addressDetails ->
                                                SubmissionDataEntity.Candidate.Details.BankAccount.Address.AddressDetails(
                                                    country = addressDetails.country.let { country ->
                                                        SubmissionDataEntity.Candidate.Details.BankAccount.Address.AddressDetails.Country(
                                                            id = country.id,
                                                            scheme = country.scheme,
                                                            description = country.description
                                                        )
                                                    },
                                                    locality = addressDetails.locality.let { locality ->
                                                        SubmissionDataEntity.Candidate.Details.BankAccount.Address.AddressDetails.Locality(
                                                            id = locality.id,
                                                            scheme = locality.scheme,
                                                            description = locality.description
                                                        )
                                                    },
                                                    region = addressDetails.region.let { region ->
                                                        SubmissionDataEntity.Candidate.Details.BankAccount.Address.AddressDetails.Region(
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
                                        SubmissionDataEntity.Candidate.Details.BankAccount.AccountIdentification(
                                            id = accountIdentification.id,
                                            scheme = accountIdentification.scheme
                                        )
                                    },
                                    additionalAccountIdentifiers = bankAccount.additionalAccountIdentifiers.map { additionalAccountIdentifier ->
                                        SubmissionDataEntity.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                            id = additionalAccountIdentifier.id,
                                            scheme = additionalAccountIdentifier.scheme
                                        )
                                    },
                                    bankName = bankAccount.bankName,
                                    identifier = bankAccount.identifier.let { identifier ->
                                        SubmissionDataEntity.Candidate.Details.BankAccount.Identifier(
                                            id = identifier.id,
                                            scheme = identifier.scheme
                                        )
                                    }
                                )
                            },
                            legalForm = details.legalForm?.let { legalForm ->
                                SubmissionDataEntity.Candidate.Details.LegalForm(
                                    id = legalForm.id,
                                    scheme = legalForm.scheme,
                                    description = legalForm.description,
                                    uri = legalForm.uri
                                )
                            },
                            mainEconomicActivities = details.mainEconomicActivities.map { mainEconomicActivity ->
                                SubmissionDataEntity.Candidate.Details.MainEconomicActivity(
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
                        SubmissionDataEntity.Candidate.Identifier(
                            id = identifier.id,
                            scheme = identifier.scheme,
                            uri = identifier.uri,
                            legalName = identifier.legalName
                        )
                    },
                    persones = candidate.persones.map { person ->
                        SubmissionDataEntity.Candidate.Person(
                            title = person.title,
                            identifier = person.identifier.let { identifier ->
                                SubmissionDataEntity.Candidate.Person.Identifier(
                                    id = identifier.id,
                                    uri = identifier.uri,
                                    scheme = identifier.scheme
                                )
                            },
                            name = person.name,
                            businessFunctions = person.businessFunctions.map { businessFunction ->
                                SubmissionDataEntity.Candidate.Person.BusinessFunction(
                                    id = businessFunction.id,
                                    documents = businessFunction.documents.map { document ->
                                        SubmissionDataEntity.Candidate.Person.BusinessFunction.Document(
                                            id = document.id,
                                            title = document.title,
                                            description = document.description,
                                            documentType = document.documentType
                                        )
                                    },
                                    jobTitle = businessFunction.jobTitle,
                                    period = businessFunction.period.let { period ->
                                        SubmissionDataEntity.Candidate.Person.BusinessFunction.Period(
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