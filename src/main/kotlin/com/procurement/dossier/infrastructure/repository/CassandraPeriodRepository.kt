package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.procurement.dossier.application.exception.DatabaseInteractionException
import com.procurement.dossier.application.repository.PeriodRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.toDate
import com.procurement.dossier.domain.util.extension.toLocal
import com.procurement.dossier.infrastructure.extension.cassandra.executeRead
import com.procurement.dossier.infrastructure.extension.cassandra.executeWrite
import com.procurement.dossier.infrastructure.extension.cassandra.tryExecute
import com.procurement.dossier.infrastructure.model.entity.PeriodEntity
import org.springframework.stereotype.Repository

@Repository
class CassandraPeriodRepository(private val session: Session) : PeriodRepository {

    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "period"
        private const val columnCpid = "cpid"
        private const val columnOcid = "ocid"
        private const val columnStartDate = "start_date"
        private const val columnEndDate = "end_date"

        private const val SAVE_NEW_PERIOD_CQL = """
               INSERT INTO $keySpace.$tableName(
                      $columnCpid,
                      $columnOcid,
                      $columnStartDate,
                      $columnEndDate
               )
               VALUES(?, ?, ?, ?)
               IF NOT EXISTS
            """

        private const val SAVE_OR_UPDATE_PERIOD_CQL = """
               INSERT INTO $keySpace.$tableName(
                      $columnCpid,
                      $columnOcid,
                      $columnStartDate,
                      $columnEndDate
               )
               VALUES(?, ?, ?, ?)
            """

        private const val FIND_BY_CPID_AND_OCID_CQL = """
               SELECT $columnCpid,
                      $columnOcid,
                      $columnStartDate,
                      $columnEndDate
                 FROM $keySpace.$tableName
                WHERE $columnCpid=? 
                  AND $columnOcid=?
            """
    }

    private val preparedFindByCpidAndOcidCQL = session.prepare(FIND_BY_CPID_AND_OCID_CQL)
    private val preparedSaveNewPeriodCQL = session.prepare(SAVE_NEW_PERIOD_CQL)
    private val preparedSaveOrUpdatePeriodCQL = session.prepare(SAVE_OR_UPDATE_PERIOD_CQL)

    override fun findBy(cpid: Cpid, ocid: Ocid): PeriodEntity? {
        val query = preparedFindByCpidAndOcidCQL.bind()
            .apply {
                setString(columnCpid, cpid.toString())
                setString(columnOcid, ocid.toString())
            }
        return executeRead(query = query)
            .one()
            ?.let { row -> converter(row = row) }
    }

    override fun tryFindBy(cpid: Cpid, ocid: Ocid): Result<PeriodEntity?, Fail.Incident> {
        val query = preparedFindByCpidAndOcidCQL.bind()
            .apply {
                setString(columnCpid, cpid.toString())
                setString(columnOcid, ocid.toString())
            }

        return query.tryExecute(session)
            .orForwardFail { error -> return error }
            .one()
            ?.let { row -> tryConvert(row = row) }
            ?.orForwardFail { error -> return error }
            .asSuccess()
    }

    private fun tryConvert(row: Row): Result<PeriodEntity, Fail.Incident>  {
        val cpid = row.getString(columnCpid)
        val cpidParsed = Cpid.tryCreateOrNull(cpid)
            ?: return failure(
                Fail.Incident.Database.Parsing(
                    column = columnCpid, value = cpid
                )
            )

        val ocid = row.getString(columnOcid)
        val ocidParsed = Ocid.tryCreateOrNull(ocid)
            ?: return failure(
                Fail.Incident.Database.Parsing(
                    column = columnOcid, value = ocid
                )
            )

        return PeriodEntity(
            cpid = cpidParsed,
            ocid = ocidParsed,
            endDate = row.getTimestamp(columnEndDate).toLocal(),
            startDate = row.getTimestamp(columnStartDate).toLocal()
        ).asSuccess()
    }

    private fun converter(row: Row): PeriodEntity {
        val cpid = row.getString(columnCpid)
        val cpidParsed = Cpid.tryCreateOrNull(cpid)
            ?: throw DatabaseInteractionException("Could not parse cpid '$cpid' from database")

        val ocid = row.getString(columnOcid)
        val ocidParsed = Ocid.tryCreateOrNull(ocid)
            ?: throw DatabaseInteractionException("Could not parse ocid '$ocid' from database")

        return PeriodEntity(
            cpid = cpidParsed,
            ocid = ocidParsed,
            endDate = row.getTimestamp(columnEndDate).toLocal(),
            startDate = row.getTimestamp(columnStartDate).toLocal()
        )
    }

    override fun saveNewPeriod(period: PeriodEntity): Boolean {
        val statement = preparedSaveNewPeriodCQL.bind()
            .apply {
                setString(columnCpid, period.cpid.toString())
                setString(columnOcid, period.ocid.toString())
                setTimestamp(columnStartDate, period.startDate.toDate())
                setTimestamp(columnEndDate, period.endDate.toDate())
            }

        return executeWrite(query = statement).wasApplied()
    }

    override fun saveOrUpdatePeriod(period: PeriodEntity) {
        val statement = preparedSaveOrUpdatePeriodCQL.bind()
            .apply {
                setString(columnCpid, period.cpid.toString())
                setString(columnOcid, period.ocid.toString())
                setTimestamp(columnStartDate, period.startDate.toDate())
                setTimestamp(columnEndDate, period.endDate.toDate())
            }

        executeWrite(query = statement)
    }

    private fun executeRead(query: BoundStatement) = query.executeRead(
        session = session,
        errorMessage = "Encountered error while reading period from database"
    )

    private fun executeWrite(query: BoundStatement) = query.executeWrite(
        session = session,
        errorMessage = "Encountered error while writing period to database"
    )
}
