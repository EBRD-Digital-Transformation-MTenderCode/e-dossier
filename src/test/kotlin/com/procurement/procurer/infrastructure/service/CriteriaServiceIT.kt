package com.procurement.procurer.infrastructure.service

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.HostDistance
import com.datastax.driver.core.PlainTextAuthProvider
import com.datastax.driver.core.PoolingOptions
import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.procurer.application.exception.DatabaseInteractionException
import com.procurement.procurer.application.model.entity.CnEntity
import com.procurement.procurer.application.repository.CriteriaRepository
import com.procurement.procurer.infrastructure.config.DatabaseTestConfiguration
import com.procurement.procurer.infrastructure.config.OCDSProperties
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandType
import com.procurement.procurer.infrastructure.model.dto.cn.CreateCriteriaResponse
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.repository.CassandraCriteriaRepository
import com.procurement.procurer.infrastructure.repository.CassandraTestContainer
import com.procurement.procurer.infrastructure.utils.toJson
import com.procurement.procurer.json.getObject
import com.procurement.procurer.json.loadJson
import com.procurement.procurer.json.toJson
import com.procurement.procurer.json.toNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class CassandraACRepositoryIT {
    companion object {
        private const val CPID = "cpid-1"
        private const val OWNER = "owner-1"
        private const val JSON_DATA = """ {"ac": "data"} """

        private const val KEYSPACE = "ocds"
        private const val TABLE_PROCURER = "procurer_tender"
        private const val COLUMN_CPID = "cp_id"
        private const val COLUMN_OWNER = "owner"
        private const val COLUMN_JSONDATA = "json_data"
    }

    @Autowired
    private lateinit var container: CassandraTestContainer

    private lateinit var session: Session
    private lateinit var ocdsProperties: OCDSProperties
    private lateinit var criteriaService: CriteriaService
    private lateinit var generationService: GenerationService
    private lateinit var criteriaRepository: CriteriaRepository

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

        session = spy(cluster.connect())

        createKeyspace()
        createTable()

        criteriaRepository = spy(CassandraCriteriaRepository(session))
        ocdsProperties = OCDSProperties()
        ocdsProperties.prefix = "ocds-t1s2t3"
        generationService = GenerationService(ocdsProperties)
        criteriaService = CriteriaService(generationService, criteriaRepository)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()
    }

    @Test
    fun `created twice`() {

        val pre_document = parseContext.parse(json)
        val pre_requestNode = pre_document.jsonString().toNode()

        pre_requestNode.getObject("tender")
            .put("awardCriteria", AwardCriteria.COST_ONLY.value)
            .put("awardCriteriaDetails", AwardCriteriaDetails.MANUAL.value)
            .remove("criteria[0].relatesTo")

        val pre_cm = commandMessage(
            CommandType.CREATE_CRITERIA,
            data = pre_requestNode
        )

        val pre_response = criteriaService.createCriteria(pre_cm)

        val responseData = pre_response.data as CreateCriteriaResponse
        println(responseData)
        assertEquals(responseData.awardCriteriaDetails, AwardCriteriaDetails.MANUAL)



        val post_document = parseContext.parse(json)
        val post_requestNode = post_document.jsonString().toNode()

        post_requestNode.getObject("tender")
            .put("awardCriteria", AwardCriteria.PRICE_ONLY.value)
            .put("awardCriteriaDetails", AwardCriteriaDetails.MANUAL.value)
            .remove("criteria[0].relatesTo")

        val post_cm = commandMessage(
            CommandType.CREATE_CRITERIA,
            data = post_requestNode
        )

        val post_response = criteriaService.createCriteria(post_cm)

        verify(criteriaRepository, times(2))
            .save(any())

        verify(criteriaRepository, times(1))
            .findBy(any())

        assertEquals(toJson(pre_response), toJson(post_response))

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
                CREATE TABLE IF NOT EXISTS  $KEYSPACE.$TABLE_PROCURER (
                    $COLUMN_CPID text,
                    $COLUMN_OWNER text,
                    $COLUMN_JSONDATA text,
                    primary key($COLUMN_CPID)
                );
            """
        )
    }


}