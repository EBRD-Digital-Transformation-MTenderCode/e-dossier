package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.HostDistance
import com.datastax.driver.core.PlainTextAuthProvider
import com.datastax.driver.core.PoolingOptions
import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.repository.SubmissionRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.model.enums.Scale
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.enums.SupplierType
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionCredentials
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.model.submission.SubmissionState
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.dossier.infrastructure.config.DatabaseTestConfiguration
import com.procurement.dossier.infrastructure.model.entity.submission.SubmissionDataEntity
import com.procurement.dossier.json.toJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class SubmissionRepositoryIT {

    companion object {
        private val CPID = Cpid.tryCreateOrNull("ocds-t1s2t3-MD-1565251033096")!!
        private val OCID = Ocid.tryCreateOrNull("ocds-b3wdp1-MD-1581509539187-EV-1581509653044")!!

        private const val KEYSPACE = "dossier"
        private const val TABLE_NAME = "submission"
        private const val COLUMN_CPID = "cpid"
        private const val COLUMN_OCID = "ocid"
        private const val COLUMN_ID = "id"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_TOKEN = "token_entity"
        private const val COLUMN_OWNER = "owner"
        private const val COLUMN_JSON_DATA = "json_data"
    }

    @Autowired
    private lateinit var container: CassandraTestContainer

    private lateinit var session: Session
    private lateinit var cassandraCluster: Cluster

    private lateinit var submissionRepository: SubmissionRepository

    @BeforeEach
    fun init() {
        val poolingOptions = PoolingOptions()
            .setMaxConnectionsPerHost(HostDistance.LOCAL, 1)
        val cluster = Cluster.builder()
            .addContactPoints(container.contractPoint)
            .withPort(container.port)
            .withoutJMXReporting()
            .withPoolingOptions(poolingOptions)
            .withAuthProvider(PlainTextAuthProvider(container.username, container.password))
            .build()

        cassandraCluster = cluster
        session = spy(cluster.connect())

        createKeyspace()
        createTable()

        submissionRepository = CassandraSubmissionRepository(session)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()

        session.close()
        cassandraCluster.closeAsync()
    }

    @Test
    fun findSubmission_success() {
        val expectedSubmission = stubSubmission()
        insertSubmission(cpid = CPID, ocid = OCID, submission = expectedSubmission)
        val actualSubmission = submissionRepository.findSubmission(
            cpid = CPID, ocid = OCID, id = expectedSubmission.id
        ).get

        assertEquals(expectedSubmission, actualSubmission)
    }

    @Test
    fun findSubmission_submissionNotFound_fail() {
        val actualSubmission = submissionRepository.findSubmission(
            cpid = CPID, ocid = OCID, id = SubmissionId.create(UUID.randomUUID().toString())
        ).get

        assertTrue(actualSubmission == null)
    }

    @Test
    fun findSubmission_executeException_fail() {
        doThrow(RuntimeException())
            .whenever(session)
            .execute(any<BoundStatement>())

        val expected = submissionRepository.findSubmission(cpid = CPID, ocid = OCID, id = SubmissionId.create(UUID.randomUUID().toString())).error

        assertTrue(expected is Fail.Incident.Database.Interaction)
    }

    @Test
    fun saveSubmission_success() {
        val expectedSubmission = stubSubmission()
        submissionRepository.saveSubmission(cpid = CPID, ocid = OCID, submission = expectedSubmission)
        val actualSubmission = submissionRepository.findSubmission(
            cpid = CPID, ocid = OCID, id = expectedSubmission.id
        ).get

        assertEquals(expectedSubmission, actualSubmission)
    }

    @Test
    fun saveSubmissionsBatch_success() {
        val expectedSubmission1 = stubSubmission()
        val expectedSubmission2 = stubSubmission()

        submissionRepository.saveAll(cpid = CPID, ocid = OCID, submissions = listOf(expectedSubmission1, expectedSubmission2))

        val actualSubmission1 = submissionRepository
            .findSubmission(cpid = CPID, ocid = OCID, id = expectedSubmission1.id).get

        val actualSubmission2 = submissionRepository
            .findSubmission(cpid = CPID, ocid = OCID, id = expectedSubmission2.id).get

        assertEquals(expectedSubmission1, actualSubmission1)
        assertEquals(expectedSubmission2, actualSubmission2)
    }

    @Test
    fun saveSubmission_executeException_fail() {
        val submission = stubSubmission()
        doThrow(RuntimeException())
            .whenever(session)
            .execute(any<BoundStatement>())

        val expected = submissionRepository.saveSubmission(cpid = CPID, ocid = OCID, submission = submission).error

        assertTrue(expected is Fail.Incident.Database.Interaction)
    }

    @Test
    fun getSubmissionsStates_success() {
        val firstSubmission = stubSubmission()
        val secondSubmission = stubSubmission().copy(id = SubmissionId.create(UUID.randomUUID().toString()), status = SubmissionStatus.WITHDRAWN)

        insertSubmission(cpid = CPID, ocid = OCID, submission = firstSubmission)
        insertSubmission(cpid = CPID, ocid = OCID, submission = secondSubmission)

        val actual = submissionRepository.getSubmissionsStates(
            cpid = CPID, ocid = OCID, submissionIds = listOf(firstSubmission.id, secondSubmission.id)
        ).get

        val expected = setOf(
            SubmissionState(id = firstSubmission.id, status = firstSubmission.status),
            SubmissionState(id = secondSubmission.id, status = secondSubmission.status)
        )
        assertEquals(expected.size, actual.size)
        assertEquals(expected, actual.toSet())
    }

    @Test
    fun getSubmissionsStates_submissionNotFound_success() {
        val actual = submissionRepository.getSubmissionsStates(
            cpid = CPID,
            ocid = OCID,
            submissionIds = listOf(
                SubmissionId.create(UUID.randomUUID().toString()),
                SubmissionId.create(UUID.randomUUID().toString())
            )
        ).get

        assertTrue(actual.isEmpty())
    }

    @Test
    fun getSubmissionCredentials_success() {
        val firstSubmission = stubSubmission()
        insertSubmission(cpid = CPID, ocid = OCID, submission = firstSubmission)

        val actual = submissionRepository.getSubmissionCredentials(
            cpid = CPID, ocid = OCID, id = firstSubmission.id
        ).get

        val expected = SubmissionCredentials(
            id = firstSubmission.id,
            owner = firstSubmission.owner,
            token = firstSubmission.token
        )

        assertEquals(expected, actual)
    }

    @Test
    fun getSubmissionCredentials_submissionNotFound_success() {
        val actual = submissionRepository.getSubmissionCredentials(
            cpid = CPID, ocid = OCID, id = SubmissionId.create(UUID.randomUUID().toString())
        ).get

        assertTrue(actual == null)
    }

    @Test
    fun updateSubmission_success() {
        val initialSubmission = stubSubmission()
        insertSubmission(cpid = CPID, ocid = OCID, submission = initialSubmission)
        val submissionExpected = initialSubmission.copy(
            status = SubmissionStatus.DISQUALIFIED,
            owner = UUID.randomUUID(),
            documents = listOf(
                Submission.Document(
                    documentType = DocumentType.ILLUSTRATION,
                    id = "newId",
                    title = "newTitle",
                    description = null
                )
            )
        )
        submissionRepository.updateSubmission(cpid = CPID, ocid = OCID, submission = submissionExpected).get

        val actual = submissionRepository.findSubmission(cpid = CPID, ocid = OCID, id = initialSubmission.id).get

        assertEquals(submissionExpected, actual)
    }

    @Test
    fun findBySubmissionIds_success() {
        val firstExpectedSubmission = stubSubmission()
        val secondExpectedSubmission = stubSubmission()
        val additionalSubmission = stubSubmission()

        insertSubmission(cpid = CPID, ocid = OCID, submission = firstExpectedSubmission)
        insertSubmission(cpid = CPID, ocid = OCID, submission = secondExpectedSubmission)
        insertSubmission(cpid = CPID, ocid = OCID, submission = additionalSubmission)

        val expectedSubmissions = setOf(firstExpectedSubmission, secondExpectedSubmission)

        val actualSubmissions = submissionRepository.findBy(
            cpid = CPID, ocid = OCID, submissionIds = expectedSubmissions.map { it.id }
        ).get

        assertEquals(expectedSubmissions, actualSubmissions.toSet())
        assertTrue(actualSubmissions.size == 2)
    }
    @Test
    fun findBySubmissionIds_submissionNotFound_success() {
        val actualSubmission = submissionRepository.findBy(
            cpid = CPID, ocid = OCID, submissionIds = listOf(SubmissionId.create(UUID.randomUUID().toString()))
        ).get

        assertTrue(actualSubmission.isEmpty())
    }

    @Test
    fun findBySubmissionIds_executeException_fail() {
        doThrow(RuntimeException())
            .whenever(session)
            .execute(any<BoundStatement>())

        val expected = submissionRepository.findBy(
            cpid = CPID, ocid = OCID, submissionIds = listOf(SubmissionId.create(UUID.randomUUID().toString()))
        ).error

        assertTrue(expected is Fail.Incident.Database.Interaction)
    }

    @Test
    fun updateSubmission_SubmissionNotFound() {
        val isUpdated = submissionRepository.updateSubmission(
            cpid = CPID,
            ocid = OCID,
            submission = stubSubmission()
        ).get

        assertFalse(isUpdated)
    }

    private fun createKeyspace() {
        session.execute(
            "CREATE KEYSPACE $KEYSPACE " +
                "WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1};"
        )
    }

    private fun dropKeyspace() {
        session.execute("DROP KEYSPACE $KEYSPACE;")
    }

    private fun createTable() {
        session.execute(
            """
                CREATE TABLE IF NOT EXISTS $KEYSPACE.$TABLE_NAME
                    (
                        $COLUMN_CPID text,
                        $COLUMN_OCID text,
                        $COLUMN_ID text,
                        $COLUMN_STATUS text,
                        $COLUMN_OWNER uuid,
                        $COLUMN_TOKEN uuid,
                        $COLUMN_JSON_DATA text,
                        primary key($COLUMN_CPID, $COLUMN_OCID, $COLUMN_ID)
                    );
            """
        )
    }

    private fun insertSubmission(cpid: Cpid, ocid: Ocid, submission: Submission) {
        val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
            .value(COLUMN_CPID, cpid.toString())
            .value(COLUMN_OCID, ocid.toString())
            .value(COLUMN_ID, submission.id.toString())
            .value(COLUMN_STATUS, submission.status.key)
            .value(COLUMN_TOKEN, submission.token)
            .value(COLUMN_OWNER, submission.owner)
            .value(COLUMN_JSON_DATA, submission.convertToEntity().toJson())
        session.execute(record)
    }

    private fun Submission.convertToEntity() =
        SubmissionDataEntity(
            id = id.toString(),
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
                    value = requirementResponse.value,
                    evidences = requirementResponse.evidences
                        .map { evidence ->
                            SubmissionDataEntity.RequirementResponse.Evidence(
                                id = evidence.id,
                                description = evidence.description,
                                title = evidence.title,
                                relatedDocument = evidence.relatedDocument
                                    ?.let { relatedDocument ->
                                        SubmissionDataEntity.RequirementResponse.Evidence.RelatedDocument(
                                            id = relatedDocument.id
                                        )
                                    }
                            )
                        }
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
                    ),
                    evidences = listOf(
                        Submission.RequirementResponse.Evidence(
                            id = "evidence.id",
                            title = "evidence.title",
                            description = "evidence.description",
                            relatedDocument = Submission.RequirementResponse.Evidence.RelatedDocument(id = "document.id")
                        )
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
                                    type = BusinessFunctionType.CONTACT_POINT,
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


