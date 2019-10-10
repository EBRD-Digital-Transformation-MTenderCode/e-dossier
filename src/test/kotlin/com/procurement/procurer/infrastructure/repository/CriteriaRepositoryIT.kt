package com.procurement.procurer.infrastructure.repository

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
import com.procurement.procurer.application.exception.DatabaseInteractionException
import com.procurement.procurer.application.model.entity.CnEntity
import com.procurement.procurer.application.repository.CriteriaRepository
import com.procurement.procurer.infrastructure.config.DatabaseTestConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
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
    private lateinit var criteriaRepository: CriteriaRepository

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

        criteriaRepository = CassandraCriteriaRepository(session)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()
    }

    @Test
    fun findBy() {
        insertCN()

        val actualFundedCN: CnEntity? = criteriaRepository.findBy(cpid = CPID)

        assertNotNull(actualFundedCN)
        assertEquals(expectedFundedCN(), actualFundedCN)
    }

    @Test
    fun cnNotFound() {
        val actualFundedCN = criteriaRepository.findBy(cpid = "UNKNOWN")
        assertNull(actualFundedCN)
    }

    @Test
    fun `error while saving`() {
        val cnentity = CnEntity(
            cpid = "cpid-1",
            owner = "owner-1",
            jsonData = "{data: \"json-data-1\" }"
        )

        doThrow(RuntimeException())
            .whenever(session)
            .execute(any<BoundStatement>())

        val exception = assertThrows<DatabaseInteractionException> {
            criteriaRepository.save(cnentity)
        }
    }

    @Test
    fun `error while finding`() {
        val cpid = "cpid-1"

        doThrow(RuntimeException())
            .whenever(session)
            .execute(any<BoundStatement>())

        val exception = assertThrows<DatabaseInteractionException> {
            criteriaRepository.findBy(cpid)
        }
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

    private fun insertCN(
        cpid: String = CPID,
        owner: String = OWNER,
        jsonData: String = JSON_DATA
    ) {
        val record = QueryBuilder.insertInto(KEYSPACE, TABLE_PROCURER)
            .value(COLUMN_CPID, cpid)
            .value(COLUMN_OWNER, owner)
            .value(COLUMN_JSONDATA, jsonData)
        session.execute(record)
    }

    private fun expectedFundedCN() = CnEntity(
        cpid = CPID,
        owner = OWNER,
        jsonData = JSON_DATA
    )

}