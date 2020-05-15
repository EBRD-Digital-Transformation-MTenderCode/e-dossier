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
import com.procurement.dossier.application.repository.PeriodRulesRepository
import com.procurement.dossier.infrastructure.config.DatabaseTestConfiguration
import com.procurement.dossier.infrastructure.exception.io.ReadEntityException
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class PeriodRulesRepositoryIT {
    companion object {
        private const val KEYSPACE = "dossier"
        private const val TABLE_NAME = "period_rules"
        private const val COLUMN_COUNTRY = "country"
        private const val COLUMN_PMD = "pmd"
        private const val COLUMN_VALUE = "value"

        private val PMD = ProcurementMethod.GPA
        private val COUNTRY = "country"
        private val VALUE: Long = 1
    }

    @Autowired
    private lateinit var container: CassandraTestContainer

    private lateinit var session: Session
    private lateinit var periodRulesRepository: PeriodRulesRepository

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

        periodRulesRepository = CassandraPeriodRulesRepository(session)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()
    }

    @Test
    fun findBy() {
        insertPeriodRule(pmd = PMD, country = COUNTRY, value = VALUE)

        val actualValue = periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY)

        assertEquals(actualValue, VALUE)
    }

    @Test
    fun ruleNotFound() {
        val actualValue = periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY)

        assertTrue(actualValue == null)
    }

    @Test
    fun `error while finding`() {
        doThrow(RuntimeException())
            .whenever(session)
            .execute(any<BoundStatement>())

        assertThrows<ReadEntityException> {
            periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY)
        }
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
                    $COLUMN_VALUE bigint,
                    primary key($COLUMN_COUNTRY, $COLUMN_PMD)
                );
            """
        )
    }

    private fun insertPeriodRule(pmd: ProcurementMethod, country: String, value: Long) {
        val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
            .value(COLUMN_COUNTRY, country)
            .value(COLUMN_PMD, pmd.name)
            .value(COLUMN_VALUE, value)
        session.execute(record)
    }
}
