package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.SubmissionRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionCredentials
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.model.submission.SubmissionState
import com.procurement.dossier.domain.util.MaybeFail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.bind
import com.procurement.dossier.infrastructure.extension.cassandra.tryExecute
import com.procurement.dossier.infrastructure.model.entity.submission.SubmissionDataEntity
import com.procurement.dossier.infrastructure.utils.tryToJson
import com.procurement.dossier.infrastructure.utils.tryToObject
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
        private const val columnToken = "token_entity"
        private const val columnOwner = "owner"
        private const val columnJsonData = "json_data"
        private const val idValues = "id_values"

        private const val SAVE_SUBMISSION_CQL = """
               INSERT INTO $keySpace.$tableName(
                      $columnCpid,
                      $columnOcid,
                      $columnId,
                      $columnStatus,
                      $columnToken,
                      $columnOwner,
                      $columnJsonData
               )
               VALUES(?, ?, ?, ?, ?, ?, ?)                
            """

        private const val GET_SUBMISSION_STATUS_CQL = """
               SELECT $columnStatus,
                      $columnId
                 FROM $keySpace.$tableName
                WHERE $columnCpid=? 
                  AND $columnOcid=?
                  AND $columnId IN :$idValues;
            """

        private const val UPDATE_SUBMISSION_CQL = """
               UPDATE $keySpace.$tableName
                  SET $columnStatus=?,
                      $columnToken =?,
                      $columnOwner =?,
                      $columnJsonData =?
                WHERE $columnCpid=? 
                  AND $columnOcid=?
                  AND $columnId=?               
               IF EXISTS
            """

        private const val GET_SUBMISSION_CREDENTIALS_CQL = """
               SELECT $columnToken,
                      $columnOwner,
                      $columnId
                 FROM $keySpace.$tableName
                WHERE $columnCpid=? 
                  AND $columnOcid=?
                  AND $columnId=?
            """

        private const val FIND_SUBMISSION_CQL = """
               SELECT $columnStatus,
                      $columnToken,
                      $columnOwner,
                      $columnJsonData
                 FROM $keySpace.$tableName
                WHERE $columnCpid=? 
                  AND $columnOcid=?
                  AND $columnId=?;
            """
    }

    private val preparedSaveSubmissionCQL = session.prepare(SAVE_SUBMISSION_CQL)
    private val preparedGetSubmissionStatusCQL = session.prepare(GET_SUBMISSION_STATUS_CQL)
    private val preparedUpdateSubmissionCQL = session.prepare(UPDATE_SUBMISSION_CQL)
    private val preparedGetSubmissionCredentialsCQL = session.prepare(GET_SUBMISSION_CREDENTIALS_CQL)
    private val preparedFindSubmissionCQL = session.prepare(FIND_SUBMISSION_CQL)

    override fun saveSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): MaybeFail<Fail.Incident> {
        val entity = submission.convert()
        val jsonData = tryToJson(entity).doReturn { incident -> return MaybeFail.fail(incident) }
        val statement = preparedSaveSubmissionCQL.bind()
            .apply {
                setString(columnCpid, cpid.toString())
                setString(columnOcid, ocid.toString())
                setUUID(columnId, submission.id)
                setString(columnStatus, submission.status.key)
                setUUID(columnToken, submission.token)
                setUUID(columnOwner, submission.owner)
                setString(columnJsonData, jsonData)
            }

        statement.tryExecute(session).doReturn { fail -> return MaybeFail.fail(fail) }
        return MaybeFail.none()
    }

    private fun Submission.convert() =
        SubmissionDataEntity(
            id = id,
            date = date,
            status = status,
            token = token,
            owner = owner,
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
                                            description = country.description,
                                            uri = country.uri
                                        )
                                    },
                                    locality = addressDetails.locality.let { locality ->
                                        SubmissionDataEntity.Candidate.Address.AddressDetails.Locality(
                                            id = locality.id,
                                            scheme = locality.scheme,
                                            description = locality.description,
                                            uri = locality.uri
                                        )
                                    },
                                    region = addressDetails.region.let { region ->
                                        SubmissionDataEntity.Candidate.Address.AddressDetails.Region(
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
                            id = person.id,
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

    override fun getSubmissionsStates(
        cpid: Cpid, ocid: Ocid, submissionIds: List<SubmissionId>
    ): Result<List<SubmissionState>, Fail.Incident> {
        val query = preparedGetSubmissionStatusCQL.bind()
            .setList(idValues, submissionIds)
            .setString(columnCpid, cpid.toString())
            .setString(columnOcid, ocid.toString())

        return query.tryExecute(session)
            .orForwardFail { error -> return error }
            .map { row -> convertToState(row = row).orForwardFail { fail -> return fail } }
            .asSuccess()
    }

    private fun convertToState(row: Row): Result<SubmissionState, Fail.Incident.Database.Parsing> {
        val id = row.getUUID(columnId)
        val status = row.getString(columnStatus)
        val statusParsed = SubmissionStatus.orNull(row.getString(columnStatus))
            ?: return Fail.Incident.Database.Parsing(column = columnStatus, value = status).asFailure()
        return SubmissionState(id = id, status = statusParsed).asSuccess()
    }

    override fun updateSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): Result<Boolean, Fail.Incident> {
        val entity = submission.convert()
        val jsonData = tryToJson(entity).orForwardFail { fail -> return fail }

        val statement = preparedUpdateSubmissionCQL.bind()
            .apply {
                setString(columnCpid, cpid.toString())
                setString(columnOcid, ocid.toString())
                setUUID(columnId, submission.id)
                setString(columnStatus, submission.status.key)
                setUUID(columnToken, submission.token)
                setUUID(columnOwner, submission.owner)
                setString(columnJsonData, jsonData)
            }

        return statement.tryExecute(session).bind { resultSet ->
            resultSet.wasApplied().asSuccess<Boolean, Fail.Incident>()
        }
    }

    override fun getSubmissionCredentials(
        cpid: Cpid,
        ocid: Ocid,
        id: SubmissionId
    ): Result<SubmissionCredentials?, Fail.Incident> {
        val query = preparedGetSubmissionCredentialsCQL.bind()
            .setUUID(columnId, id)
            .setString(columnCpid, cpid.toString())
            .setString(columnOcid, ocid.toString())

        return query.tryExecute(session)
            .orForwardFail { error -> return error }
            .one()
            ?.let { row -> convertToCredentials(row = row) }
            .asSuccess()
    }

    private fun convertToCredentials(row: Row): SubmissionCredentials {
        val id = row.getUUID(columnId)
        val token = row.getUUID(columnToken)
        val owner = row.getUUID(columnOwner)
        return SubmissionCredentials(id = id, token = token, owner = owner)
    }

    override fun findSubmission(cpid: Cpid, ocid: Ocid, id: SubmissionId): Result<Submission?, Fail.Incident> {
        val query = preparedFindSubmissionCQL.bind()
            .setUUID(columnId, id)
            .setString(columnCpid, cpid.toString())
            .setString(columnOcid, ocid.toString())

        return query.tryExecute(session)
            .orForwardFail { fail -> return fail }
            .one()
            ?.let { row -> convertToSubmission(row = row) }
            ?.orForwardFail { fail -> return fail }
            .asSuccess()
    }

    private fun convertToSubmission(row: Row): Result<Submission, Fail.Incident> {
        val submissionEntity = row.getString(columnJsonData).tryToObject(SubmissionDataEntity::class.java)
            .orForwardFail { fail -> return fail }

        return createSubmission(submissionEntity = submissionEntity).asSuccess()
    }

    private fun createSubmission(
        submissionEntity: SubmissionDataEntity
    ) = Submission(
        id = submissionEntity.id,
        date = submissionEntity.date,
        status = submissionEntity.status,
        token = submissionEntity.token,
        owner = submissionEntity.owner,
        requirementResponses = submissionEntity.requirementResponses?.map { requirementResponse ->
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
        }.orEmpty(),
        documents = submissionEntity.documents?.map { document ->
            Submission.Document(
                id = document.id,
                description = document.description,
                documentType = document.documentType,
                title = document.title
            )
        }.orEmpty(),
        candidates = submissionEntity.candidates.map { candidate ->
            Submission.Candidate(
                id = candidate.id,
                name = candidate.name,
                additionalIdentifiers = candidate.additionalIdentifiers?.map { additionalIdentifier ->
                    Submission.Candidate.AdditionalIdentifier(
                        id = additionalIdentifier.id,
                        legalName = additionalIdentifier.legalName,
                        scheme = additionalIdentifier.scheme,
                        uri = additionalIdentifier.uri
                    )
                }.orEmpty(),
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
                        bankAccounts = details.bankAccounts?.map { bankAccount ->
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
                                additionalAccountIdentifiers = bankAccount.additionalAccountIdentifiers?.map { additionalAccountIdentifier ->
                                    Submission.Candidate.Details.BankAccount.AdditionalAccountIdentifier(
                                        id = additionalAccountIdentifier.id,
                                        scheme = additionalAccountIdentifier.scheme
                                    )
                                }.orEmpty(),
                                bankName = bankAccount.bankName,
                                identifier = bankAccount.identifier.let { identifier ->
                                    Submission.Candidate.Details.BankAccount.Identifier(
                                        id = identifier.id,
                                        scheme = identifier.scheme
                                    )
                                }
                            )
                        }.orEmpty(),
                        legalForm = details.legalForm?.let { legalForm ->
                            Submission.Candidate.Details.LegalForm(
                                id = legalForm.id,
                                scheme = legalForm.scheme,
                                description = legalForm.description,
                                uri = legalForm.uri
                            )
                        },
                        mainEconomicActivities = details.mainEconomicActivities?.map { mainEconomicActivity ->
                            Submission.Candidate.Details.MainEconomicActivity(
                                id = mainEconomicActivity.id,
                                uri = mainEconomicActivity.uri,
                                description = mainEconomicActivity.description,
                                scheme = mainEconomicActivity.scheme
                            )
                        }.orEmpty(),
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
                persones = candidate.persones?.map { person ->
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
                                documents = businessFunction.documents?.map { document ->
                                    Submission.Candidate.Person.BusinessFunction.Document(
                                        id = document.id,
                                        title = document.title,
                                        description = document.description,
                                        documentType = document.documentType
                                    )
                                }.orEmpty(),
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
                }.orEmpty()
            )
        }
    )
}