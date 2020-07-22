package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.RulesRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.tryToLong
import com.procurement.dossier.infrastructure.extension.cassandra.executeRead
import com.procurement.dossier.infrastructure.extension.cassandra.tryExecute
import com.procurement.dossier.infrastructure.model.dto.ocds.Operation
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class CassandraRulesRepository(private val session: Session) : RulesRepository {
    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "rules"
        private const val columnCountry = "country"
        private const val columnPmd = "pmd"
        private const val columnOperationType = "operationType"
        private const val columnParameter = "parameter"
        private const val columnValue = "value"

        private const val FIND_BY_CQL = """
               SELECT $columnValue
                 FROM $keySpace.$tableName
                WHERE $columnCountry=? 
                  AND $columnPmd=?
                  AND $columnOperationType =?
                  AND $columnParameter=?
            """

        private const val PERIOD_DURATION_PARAMETER = "minSubmissionPeriodDuration"
        private const val SUBMISSIONS_MINIMUM_PARAMETER = "minQtySubmissionsForReturning"
        private const val EXTENSION_PARAMETER = "extensionAfterUnsuspended"
        private const val OPERATION_TYPE_ALL = "all"
    }

    private val preparedFindPeriodRuleCQL = session.prepare(FIND_BY_CQL)

    override fun findPeriodDuration(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation?
    ): Duration? {
        val query = preparedFindPeriodRuleCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
                setString(columnOperationType, operationType?.key ?: OPERATION_TYPE_ALL)
                setString(columnParameter, PERIOD_DURATION_PARAMETER)
            }
        return executeRead(query).one()
            ?.getString(columnValue)
            ?.toLong()
            ?.let { Duration.ofSeconds(it) }
    }

    override fun findSubmissionsMinimumQuantity(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation?
    ): Result<Long?, Fail.Incident> {
        val query = preparedFindPeriodRuleCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
                setString(columnOperationType, operationType?.key ?: OPERATION_TYPE_ALL)
                setString(columnParameter, SUBMISSIONS_MINIMUM_PARAMETER)
            }

        val minimumQuantity = query.tryExecute(session)
            .orForwardFail { fail -> return fail }
            .one()
            ?.getString(columnValue)

        return minimumQuantity
            ?.tryToLong()
            ?.doReturn { incident ->
                return Fail.Incident.Database.Parsing(
                    column = columnValue,
                    value = minimumQuantity,
                    exception = incident.exception
                ).asFailure()
            }
            .asSuccess()
    }

    override fun findExtensionAfterUnsuspended(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation?
    ): Duration? {
        val query = preparedFindPeriodRuleCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
                setString(columnOperationType, operationType?.key ?: OPERATION_TYPE_ALL)
                setString(columnParameter, EXTENSION_PARAMETER)
            }
        return executeRead(query).one()
            ?.getString(columnValue)
            ?.toLong()
            ?.let { Duration.ofSeconds(it) }
    }

    override fun findSubmissionValidState(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation
    ): Result<SubmissionStatus?, Fail.Incident> {
        val query = preparedFindPeriodRuleCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
                setString(columnOperationType, operationType.key)
                setString(columnParameter, PERIOD_DURATION_PARAMETER)
            }
        return executeRead(query).one()
            ?.getString(columnValue)
            ?.let {
                SubmissionStatus.tryOf(it)
                    .doReturn { error ->
                        return Fail.Incident.Database.Parsing(column = columnValue, value = it).asFailure()
                    }
            }
            .asSuccess()
    }

    private fun executeRead(query: BoundStatement) = query.executeRead(
        session = session,
        errorMessage = "Encountered error while reading rules from database"
    )
}
