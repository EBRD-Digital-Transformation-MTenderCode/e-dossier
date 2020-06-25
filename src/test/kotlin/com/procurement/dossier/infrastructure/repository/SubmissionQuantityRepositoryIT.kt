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
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.infrastructure.config.DatabaseTestConfiguration
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class SubmissionQuantityRepositoryIT {
    companion object {
        private const val KEYSPACE = "dossier"
        private const val TABLE_NAME = "submission_quantity"
        private const val COLUMN_COUNTRY = "country"
        private const val COLUMN_PMD = "pmd"
        private const val COLUMN_MIN_SUBMISSIONS = "min_submissions"

        private val PMD = ProcurementMethod.GPA
        private val COUNTRY = "country"
        private val QUANTITY: Long = 3
    }

    @Autowired
    private lateinit var container: CassandraTestContainer

    private lateinit var session: Session
    private lateinit var cassandraSubmissionQuantityRepository: CassandraSubmissionQuantityRepository

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

        cassandraSubmissionQuantityRepository = CassandraSubmissionQuantityRepository(session)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()
    }

    @Test
    fun findBy() {
        insertQuantity(pmd = PMD, country = COUNTRY, quantity = QUANTITY)

        val actualQuantity = cassandraSubmissionQuantityRepository.findMinimum(pmd = PMD, country = COUNTRY).get

        assertEquals(actualQuantity, QUANTITY)
    }

    @Test
    fun quantityNotFound() {
        val actualValue = cassandraSubmissionQuantityRepository.findMinimum(pmd = PMD, country = COUNTRY).get

        assertTrue(actualValue == null)
    }

    @Test
    fun `error while finding`() {
        doThrow(RuntimeException())
            .whenever(session)
            .execute(any<BoundStatement>())

         val expectedError =  cassandraSubmissionQuantityRepository.findMinimum(pmd = PMD, country = COUNTRY).error

        assertTrue(expectedError is Fail.Incident.Database.Interaction)
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
                CREATE TABLE IF NOT EXISTS  $KEYSPACE.$TABLE_NAME (
                    $COLUMN_COUNTRY text,
                    $COLUMN_PMD text,
                    $COLUMN_MIN_SUBMISSIONS bigint,
                    primary key($COLUMN_COUNTRY, $COLUMN_PMD)
                );
            """
        )
    }

    private fun insertQuantity(pmd: ProcurementMethod, country: String, quantity: Long) {
        val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
            .value(COLUMN_COUNTRY, country)
            .value(COLUMN_PMD, pmd.name)
            .value(COLUMN_MIN_SUBMISSIONS, quantity)
        session.execute(record)
    }
}
