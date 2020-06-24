package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.RulesRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.infrastructure.extension.cassandra.executeRead
import com.procurement.dossier.infrastructure.extension.cassandra.tryExecute
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.springframework.stereotype.Repository

@Repository
class CassandraRulesRepository(private val session: Session) : RulesRepository {
    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "rules"
        private const val columnCountry = "country"
        private const val columnPmd = "pmd"
        private const val columnParameter = "parameter"
        private const val columnValue = "value"

        private const val FIND_BY_CQL = """
               SELECT $columnValue
                 FROM $keySpace.$tableName
                WHERE $columnCountry=? 
                  AND $columnPmd=?
                  AND $columnParameter=?
            """

        private const val PERIOD_DURATION_PARAMETER = "period_duration"
        private const val MINIMUM_SUBMISSIONS_PARAMETER = "minimum_submissions"
    }

    private val preparedFindPeriodRuleCQL = session.prepare(FIND_BY_CQL)

    override fun findPeriodDuration(country: String, pmd: ProcurementMethod): Long? {
        val query = preparedFindPeriodRuleCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
                setString(columnParameter, PERIOD_DURATION_PARAMETER)
            }
        return executeRead(query).one()
            ?.getLong(columnValue)
    }

    override fun findSubmissionsMinimumQuantity(country: String, pmd: ProcurementMethod): Result<Long?, Fail.Incident> {
        val query = preparedFindPeriodRuleCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
                setString(columnParameter, MINIMUM_SUBMISSIONS_PARAMETER)
            }
        return query.tryExecute(session).orForwardFail { fail -> return fail }
            .one()
            ?.getLong(columnValue)
            .asSuccess()
    }

    private fun executeRead(query: BoundStatement) = query.executeRead(
        session = session,
        errorMessage = "Encountered error while reading period rules from database"
    )
}
