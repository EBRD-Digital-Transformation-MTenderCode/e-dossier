package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.PeriodRulesRepository
import com.procurement.dossier.infrastructure.extension.cassandra.executeRead
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.springframework.stereotype.Repository

@Repository
class CassandraPeriodRulesRepository(private val session: Session) : PeriodRulesRepository {
    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "period_rules"
        private const val columnCountry = "country"
        private const val columnPmd = "pmd"
        private const val columnValue = "value"

        private const val FIND_BY_COUNTRY_AND_PMD_CQL = """
               SELECT $columnValue
                 FROM $keySpace.$tableName
                WHERE $columnCountry=? 
                  AND $columnPmd=?
            """
    }

    private val preparedFindPeriodRuleCQL = session.prepare(FIND_BY_COUNTRY_AND_PMD_CQL)

    override fun findDurationBy(country: String, pmd: ProcurementMethod): Long? {
        val query = preparedFindPeriodRuleCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
            }
        return executeRead(query).one()
            ?.getLong(columnValue)
    }

    private fun executeRead(query: BoundStatement) = query.executeRead(
        session = session,
        errorMessage = "Encountered error while reading period rules from database"
    )
}
