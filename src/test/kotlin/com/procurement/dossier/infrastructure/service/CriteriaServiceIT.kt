package com.procurement.dossier.infrastructure.service

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.HostDistance
import com.datastax.driver.core.PlainTextAuthProvider
import com.datastax.driver.core.PoolingOptions
import com.datastax.driver.core.Session
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.application.service.CriteriaService
import com.procurement.dossier.application.service.JsonValidationService
import com.procurement.dossier.application.service.context.CreateCriteriaContext
import com.procurement.dossier.infrastructure.config.DatabaseTestConfiguration
import com.procurement.dossier.infrastructure.config.ObjectMapperConfiguration
import com.procurement.dossier.infrastructure.model.dto.bpe.CommandType
import com.procurement.dossier.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.dossier.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.dossier.infrastructure.repository.CassandraCriteriaRepository
import com.procurement.dossier.infrastructure.repository.CassandraTestContainer
import com.procurement.dossier.json.getObject
import com.procurement.dossier.json.loadJson
import com.procurement.dossier.json.toNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class CriteriaServiceIT {
    companion object {
        private const val KEYSPACE = "dossier"
        private const val TABLE_NAME = "tenders"
        private const val COLUMN_CPID = "cp_id"
        private const val COLUMN_OWNER = "owner"
        private const val COLUMN_JSONDATA = "json_data"
    }

    private val objectMapper: ObjectMapper = ObjectMapperConfiguration().objectMapper()

    @Autowired
    private lateinit var container: CassandraTestContainer

    private lateinit var session: Session
    private lateinit var cassandraCluster: Cluster

    private lateinit var criteriaService: CriteriaService
    private lateinit var generationService: GenerationService
    private lateinit var criteriaRepository: CriteriaRepository
    private lateinit var jsonValidationService: JsonValidationService

    private val parseContext = JsonPath.using(Configuration.defaultConfiguration())

    private val CREATE_CRITERIA_REQUEST = "json/service/criteria/create/request/request_create_criteria_full.json"

    private val json = loadJson(CREATE_CRITERIA_REQUEST)

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

        jsonValidationService = MedeiaValidationService(objectMapper)
        criteriaRepository = spy(CassandraCriteriaRepository(session))
        generationService = GenerationService()
        criteriaService = CriteriaService(
            generationService,
            criteriaRepository,
            jsonValidationService
        )
    }

    @AfterEach
    fun clean() {
        dropKeyspace()

        session.close()
        cassandraCluster.closeAsync()
    }

    @Test
    fun `created twice`() {

        val pre_document = parseContext.parse(json)
        val pre_requestNode = pre_document.jsonString().toNode()

        pre_requestNode.getObject("tender")
            .put("awardCriteria", AwardCriteria.COST_ONLY.key)
            .put("awardCriteriaDetails", AwardCriteriaDetails.MANUAL.key)
            .remove("criteria[0].relatesTo")

        val pre_cm = commandMessage(
            CommandType.CREATE_CRITERIA,
            data = pre_requestNode
        )

        val context = CreateCriteriaContext(cpid = "cpid", owner = "owner")

        val pre_response = criteriaService.createCriteria(pre_cm, context = context)

        println(pre_response)
        assertEquals(pre_response.awardCriteriaDetails, AwardCriteriaDetails.MANUAL)

        val post_document = parseContext.parse(json)
        val post_requestNode = post_document.jsonString().toNode()

        post_requestNode.getObject("tender")
            .put("awardCriteria", AwardCriteria.PRICE_ONLY.key)
            .put("awardCriteriaDetails", AwardCriteriaDetails.MANUAL.key)
            .remove("criteria[0].relatesTo")

        val post_cm = commandMessage(
            CommandType.CREATE_CRITERIA,
            data = post_requestNode
        )

        val post_response = criteriaService.createCriteria(post_cm, context = context)

        verify(criteriaRepository, times(2))
            .save(any())

        assertEquals(AwardCriteriaDetails.MANUAL, pre_response.awardCriteriaDetails)
        assertEquals(AwardCriteriaDetails.AUTOMATED, post_response.awardCriteriaDetails)
    }

    private fun createKeyspace() {
        session.execute("CREATE KEYSPACE $KEYSPACE WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1};")
    }

    private fun dropKeyspace() {
        session.execute("DROP KEYSPACE $KEYSPACE;")
    }

    private fun createTable() {
        session.execute(
            """
                CREATE TABLE IF NOT EXISTS  $KEYSPACE.$TABLE_NAME (
                    $COLUMN_CPID text,
                    $COLUMN_OWNER text,
                    $COLUMN_JSONDATA text,
                    primary key($COLUMN_CPID)
                );
            """
        )
    }
}