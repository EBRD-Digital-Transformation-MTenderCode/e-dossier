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
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.infrastructure.config.DatabaseTestConfiguration
import com.procurement.dossier.infrastructure.exception.io.ReadEntityException
import com.procurement.dossier.infrastructure.model.dto.ocds.Operation
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [DatabaseTestConfiguration::class])
class RulesRepositoryIT {
    companion object {
        private const val KEYSPACE = "dossier"
        private const val TABLE_NAME = "rules"
        private const val COLUMN_COUNTRY = "country"
        private const val COLUMN_PMD = "pmd"
        private const val COLUMN_OPERATION_TYPE = "operation_type"
        private const val COLUMN_VALUE = "value"
        private const val COLUMN_PARAMETER = "parameter"

        private const val PERIOD_DURATION_PARAMETER = "minSubmissionPeriodDuration"
        private const val SUBMISSIONS_MINIMUM_PARAMETER = "minQtySubmissionsForReturning"
        private const val EXTENSION_PARAMETER = "extensionAfterUnsuspended"
        private const val VALID_STATES_PARAMETER = "validStates"

        private val PMD = ProcurementMethod.GPA
        private val OPERATION_TYPE = Operation.START_SECOND_STAGE

        private val COUNTRY = "country"
        private val PERIOD_DURATION_VALUE = Duration.ofDays(1)
        private val SUBMISSION_MINIMUM_VALUE: Long = 1
        private val EXTENSION_VALUE = Duration.ofSeconds(10)
        private val VALID_STATES_VALUE = SubmissionStatus.PENDING
    }

    @Autowired
    private lateinit var container: CassandraTestContainer

    private lateinit var session: Session
    private lateinit var cassandraCluster: Cluster

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

        cassandraCluster = cluster
        session = spy(cluster.connect())

        createKeyspace()
        createTable()

        rulesRepository = CassandraRulesRepository(session)
    }

    @AfterEach
    fun clean() {
        dropKeyspace()

        session.close()
        cassandraCluster.closeAsync()
    }

    @Nested
    inner class FindPeriodDuration {

        @Test
        fun success() {
            insertPeriodRule(
                pmd = PMD,
                country = COUNTRY,
                value = PERIOD_DURATION_VALUE.seconds,
                operationType = OPERATION_TYPE
            )

            val actualValue = rulesRepository.findPeriodDuration(
                pmd = PMD,
                country = COUNTRY,
                operationType = OPERATION_TYPE
            )

            assertEquals(actualValue, PERIOD_DURATION_VALUE)
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

        private fun insertPeriodRule(pmd: ProcurementMethod, country: String, value: Long, operationType: Operation) {
            val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
                .value(COLUMN_COUNTRY, country)
                .value(COLUMN_PMD, pmd.name)
                .value(COLUMN_OPERATION_TYPE, operationType.key)
                .value(COLUMN_PARAMETER, PERIOD_DURATION_PARAMETER)
                .value(COLUMN_VALUE, value.toString())
            session.execute(record)
        }
    }

    @Nested
    inner class FindSubmissionsMinimumQuantity {

        @Test
        fun success() {
            insertSubmissionMinimumRule(
                pmd = PMD,
                country = COUNTRY,
                value = SUBMISSION_MINIMUM_VALUE,
                operationType = OPERATION_TYPE
            )

            val actualValue = rulesRepository.findSubmissionsMinimumQuantity(
                pmd = PMD,
                country = COUNTRY,
                operationType = OPERATION_TYPE
            ).get

            assertEquals(actualValue, SUBMISSION_MINIMUM_VALUE)
        }

        @Test
        fun ruleNotFound() {
            val actualValue =
                rulesRepository.findSubmissionsMinimumQuantity(
                    pmd = PMD,
                    country = COUNTRY,
                    operationType = Operation.CREATE_CN
                ).get

            assertTrue(actualValue == null)
        }

        @Test
        fun `error while finding`() {
            doThrow(RuntimeException())
                .whenever(session)
                .execute(any<BoundStatement>())

            val result =
                rulesRepository.findSubmissionsMinimumQuantity(
                    pmd = PMD,
                    country = COUNTRY,
                    operationType = Operation.CREATE_CN
                ).error
            assertTrue(result is Fail.Incident.Database.Interaction)
        }

        private fun insertSubmissionMinimumRule(
            pmd: ProcurementMethod,
            country: String,
            value: Long,
            operationType: Operation
        ) {
            val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
                .value(COLUMN_COUNTRY, country)
                .value(COLUMN_PMD, pmd.name)
                .value(COLUMN_OPERATION_TYPE, operationType.key)
                .value(COLUMN_PARAMETER, SUBMISSIONS_MINIMUM_PARAMETER)
                .value(COLUMN_VALUE, value.toString())
            session.execute(record)
        }
    }

    @Nested
    inner class FindExtensionAfterUnsuspended {

        @Test
        fun success() {
            insertExtensionRule(
                pmd = PMD,
                country = COUNTRY,
                value = EXTENSION_VALUE.seconds,
                operationType = OPERATION_TYPE
            )

            val actualValue = rulesRepository.findExtensionAfterUnsuspended(
                pmd = PMD,
                country = COUNTRY,
                operationType = OPERATION_TYPE
            )

            assertEquals(EXTENSION_VALUE, actualValue)
        }

        @Test
        fun ruleNotFound() {
            val actualValue = rulesRepository.findExtensionAfterUnsuspended(pmd = PMD, country = COUNTRY)

            assertTrue(actualValue == null)
        }

        @Test
        fun `error while finding`() {
            doThrow(RuntimeException())
                .whenever(session)
                .execute(any<BoundStatement>())

            assertThrows<ReadEntityException> {
                rulesRepository.findExtensionAfterUnsuspended(pmd = PMD, country = COUNTRY)
            }
        }

        private fun insertExtensionRule(
            pmd: ProcurementMethod,
            country: String,
            value: Long,
            operationType: Operation
        ) {
            val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
                .value(COLUMN_COUNTRY, country)
                .value(COLUMN_PMD, pmd.name)
                .value(COLUMN_OPERATION_TYPE, operationType.key)
                .value(COLUMN_PARAMETER, EXTENSION_PARAMETER)
                .value(COLUMN_VALUE, value.toString())
            session.execute(record)
        }
    }

    @Nested
    inner class FindSubmissionValidState {

        @Test
        fun success() {
            insertStateRule(pmd = PMD, country = COUNTRY, value = VALID_STATES_VALUE, operationType = OPERATION_TYPE)

            val actualValue = rulesRepository.findSubmissionValidState(
                pmd = PMD,
                country = COUNTRY,
                operationType = OPERATION_TYPE
            ).get

            assertEquals(VALID_STATES_VALUE, actualValue)
        }

        @Test
        fun ruleNotFound() {
            val actualValue = rulesRepository.findSubmissionValidState(
                pmd = PMD,
                country = COUNTRY,
                operationType = OPERATION_TYPE
            ).get

            assertTrue(actualValue == null)
        }

        @Test
        fun `error while finding`() {
            doThrow(RuntimeException())
                .whenever(session)
                .execute(any<BoundStatement>())

            val result = rulesRepository.findSubmissionValidState(pmd = PMD, country = COUNTRY, operationType = OPERATION_TYPE).error

            assertTrue(result is Fail.Incident.Database.Interaction)
        }

        private fun insertStateRule(pmd: ProcurementMethod, country: String, value: SubmissionStatus, operationType: Operation) {
            val record = QueryBuilder.insertInto(KEYSPACE, TABLE_NAME)
                .value(COLUMN_COUNTRY, country)
                .value(COLUMN_PMD, pmd.name)
                .value(COLUMN_OPERATION_TYPE, operationType.key)
                .value(COLUMN_PARAMETER, VALID_STATES_PARAMETER)
                .value(COLUMN_VALUE, value.key)
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
                    $COLUMN_OPERATION_TYPE text,
                    $COLUMN_VALUE text,
                    $COLUMN_PARAMETER text,
                    primary key($COLUMN_COUNTRY, $COLUMN_PMD, $COLUMN_OPERATION_TYPE, $COLUMN_PARAMETER)
                );
            """
        )
    }
}
