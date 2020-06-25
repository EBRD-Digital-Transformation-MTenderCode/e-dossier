package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.SubmissionQuantityRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.infrastructure.extension.cassandra.tryExecute
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.springframework.stereotype.Repository

@Repository
class CassandraSubmissionQuantityRepository(private val session: Session) : SubmissionQuantityRepository {
    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "submission_quantity"
        private const val columnCountry = "country"
        private const val columnPmd = "pmd"
        private const val columnMinSubmissions = "min_submissions"

        private const val FIND_MIN_SUBMISSIONS_CQL = """
               SELECT $columnMinSubmissions
                 FROM $keySpace.$tableName
                WHERE $columnCountry=? 
                  AND $columnPmd=?
            """
    }

    private val preparedFindMinSubmissionsCQL = session.prepare(FIND_MIN_SUBMISSIONS_CQL)

    override fun findMinimum(country: String, pmd: ProcurementMethod): Result<Long?, Fail.Incident> {
        val query = preparedFindMinSubmissionsCQL.bind()
            .apply {
                setString(columnCountry, country)
                setString(columnPmd, pmd.name)
            }
        return query.tryExecute(session).orForwardFail { fail -> return fail }
            .one()
            ?.getLong(columnMinSubmissions)
            .asSuccess()
    }
}