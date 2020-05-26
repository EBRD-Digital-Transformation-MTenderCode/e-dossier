package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.procurement.dossier.application.exception.DatabaseInteractionException
import com.procurement.dossier.application.model.entity.CnEntity
import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import org.springframework.stereotype.Repository

@Repository
class CassandraCriteriaRepository(private val session: Session) : CriteriaRepository {

    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "tenders"
        private const val columnCpid = "cp_id"
        private const val columnOwner = "owner"
        const val columnJsonData = "json_data"

        private const val FIND_BY_CPID_CQL = """
               SELECT $columnCpid,
                      $columnOwner,
                      $columnJsonData
                 FROM $keySpace.$tableName
                WHERE $columnCpid=?
            """

        private const val SAVE_CN_CQL = """
               INSERT INTO $keySpace.$tableName
                     ($columnCpid,
                      $columnOwner,
                      $columnJsonData)
                  VALUES ( ?, ?, ? )
            """

        private const val UPDATE_CN_CQL = """
               UPDATE $keySpace.$tableName
                 SET   $columnJsonData=?
                 WHERE $columnCpid=?
            """
    }

    private val preparedFindByCpidCQL = session.prepare(FIND_BY_CPID_CQL)
    private val preparedSaveNewCnCQL = session.prepare(SAVE_CN_CQL)
    private val preparedUpdateCnCQL = session.prepare(UPDATE_CN_CQL)

    override fun findBy(cpid: String): CnEntity? {
        val query = preparedFindByCpidCQL.bind()
            .apply {
                setString(columnCpid, cpid)
            }

        val resultSet = load(query)
        return resultSet.one()
            ?.let { convertToContractEntity(it) }
    }

    override fun tryFindBy(cpid: Cpid): Result<CnEntity?, Fail.Incident> {
        val query = preparedFindByCpidCQL.bind()
            .apply {
                setString(columnCpid, cpid.toString())
            }
        return query.tryLoad()
            .doOnError { error -> return error.asFailure() }
            .get
            .one()
            ?.let { convertToContractEntity(it) }
            .asSuccess()
    }

    protected fun BoundStatement.tryLoad(): Result<ResultSet, Fail.Incident> = try {
        Result.success(session.execute(this))
    } catch (expected: Exception) {
        Result.failure(Fail.Incident.Database.Interaction(exception = expected))
    }

    protected fun load(statement: BoundStatement): ResultSet = try {
        session.execute(statement)
    } catch (exception: Exception) {
        throw DatabaseInteractionException(message = "Error read Contract(s) from the database.", cause = exception)
    }

    private fun convertToContractEntity(row: Row): CnEntity = CnEntity(
        cpid = row.getString(columnCpid),
        owner = row.getString(columnOwner),
        jsonData = row.getString(columnJsonData)
    )

    override fun save(cn: CnEntity): Boolean {
        val statements = preparedSaveNewCnCQL.bind()
            .apply {
                setString(columnCpid, cn.cpid)
                setString(columnOwner, cn.owner)
                setString(columnJsonData, cn.jsonData)
            }

        return saveCn(statements).wasApplied()
    }

    override fun trySave(cn: CnEntity): Result<CnEntity, Fail.Incident> {
        val statements = preparedSaveNewCnCQL.bind()
            .apply {
                setString(columnCpid, cn.cpid)
                setString(columnOwner, cn.owner)
                setString(columnJsonData, cn.jsonData)
            }
        statements.tryLoad()
            .doOnError { error -> return error.asFailure() }

        return cn.asSuccess()
    }

    override fun update(cpid: String, json: String) {
        val statements = preparedUpdateCnCQL.bind()
            .apply {
                setString(columnJsonData, json)
                setString(columnCpid, cpid)
            }

        saveCn(statements)
    }


    private fun saveCn(statement: BoundStatement): ResultSet = try {
        session.execute(statement)
    } catch (exception: Exception) {
        throw DatabaseInteractionException(message = "Error writing cancelled contract.", cause = exception)
    }
}
