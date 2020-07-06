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
import com.procurement.dossier.application.repository.RulesRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.infrastructure.config.DatabaseTestConfiguration
import com.procurement.dossier.infrastructure.exception.io.ReadEntityException
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
class RulesRepositoryIT {
    companion object {
        private const val KEYSPACE = "dossier"
        private const val TABLE_NAME = "rules"
        private const val COLUMN_COUNTRY = "country"
        private const val COLUMN_PMD = "pmd"
        private const val COLUMN_VALUE = "value"
        private const val COLUMN_PARAMETER = "parameter"

        private const val PERIOD_DURATION_PARAMETER = "period_duration"
        private const val SUBMISSIONS_MINIMUM_PARAMETER = "minQtySubmissionsForOpening"
        private val PMD = ProcurementMethod.GPA
        private val COUNTRY = "country"
        private val PERIOD_VALUE: Long = 1
        private val SUBMISSION_MINIMUM_VALUE: Long = 1
    }

    @Autowired
    private lateinit var container: CassandraTestContainer

    private lateinit var session: Session
    private lateinit var rulesRepository: RulesRepository

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

        rulesRepository = CassandraRulesRepository(session)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()
    }

    @Nested
    inner class FindPeriodDuration {

        @Test
        fun success() {
            insertPeriodRule(pmd = PMD, country = COUNTRY, value = PERIOD_VALUE)

            val actualValue = rulesRepository.findPeriodDuration(pmd = PMD, country = COUNTRY)

            assertEquals(actualValue, PERIOD_VALUE)
        }

        @Test
        fun ruleNotFound() {
            val actualValue = rulesRepository.findPeriodDuration(pmd = PMD, country = COUNTRY)

            assertTrue(actualValue == null)
        }

        @Test
        fun `error while finding`() {
            doThrow(RuntimeException())
                .whenever(session)
                .execute(any<BoundStatement>())

            assertThrows<ReadEntityException> {
                rulesRepository.findPeriodDuration(pmd = PMD, country = COUNTRY)
            }
        }

        private fun insertPeriodRule(pmd: ProcurementMethod, country: String, value: Long) {
            val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
                .value(COLUMN_COUNTRY, country)
                .value(COLUMN_PMD, pmd.name)
                .value(COLUMN_PARAMETER, PERIOD_DURATION_PARAMETER)
                .value(COLUMN_VALUE, value.toString())
            session.execute(record)
        }
    }

    @Nested
    inner class FindSubmissionsMinimumQuantity {

        @Test
        fun success() {
            insertSubmissionMinimumRule(pmd = PMD, country = COUNTRY, value = SUBMISSION_MINIMUM_VALUE)

            val actualValue = rulesRepository.findSubmissionsMinimumQuantity(pmd = PMD, country = COUNTRY).get

            assertEquals(actualValue, SUBMISSION_MINIMUM_VALUE)
        }

        @Test
        fun ruleNotFound() {
            val actualValue = rulesRepository.findSubmissionsMinimumQuantity(pmd = PMD, country = COUNTRY).get

            assertTrue(actualValue == null)
        }

        @Test
        fun `error while finding`() {
            doThrow(RuntimeException())
                .whenever(session)
                .execute(any<BoundStatement>())

            val result = rulesRepository.findSubmissionsMinimumQuantity(pmd = PMD, country = COUNTRY).error
            assertTrue(result is Fail.Incident.Database.Interaction)
        }

        private fun insertSubmissionMinimumRule(pmd: ProcurementMethod, country: String, value: Long) {
            val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
                .value(COLUMN_COUNTRY, country)
                .value(COLUMN_PMD, pmd.name)
                .value(COLUMN_PARAMETER, SUBMISSIONS_MINIMUM_PARAMETER)
                .value(COLUMN_VALUE, value.toString())
            session.execute(record)
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
                    $COLUMN_VALUE text,
                    $COLUMN_PARAMETER text,
                    primary key($COLUMN_COUNTRY, $COLUMN_PMD, $COLUMN_PARAMETER)
                );
            """
        )
    }
}
