package com.procurement.procurer.infrastructure.repository

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.procurement.procurer.application.exception.DatabaseInteractionException
import com.procurement.procurer.application.model.entity.CnEntity
import com.procurement.procurer.application.repository.CriteriaRepository
import org.springframework.stereotype.Repository

@Repository
class CassandraCriteriaRepository(private val session: Session) : CriteriaRepository {

    companion object {
        private const val keySpace = "ocds"
        private const val tableName = "procurer_tender"
        private const val columnCpid = "cp_id"
        private const val columnOwner = "owner"
        private const val columnJsonData = "json_data"

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
                IF NOT EXISTS
            """
    }

    private val preparedFindByCpidCQL = session.prepare(FIND_BY_CPID_CQL)
    private val preparedSaveCnIdCQL = session.prepare(SAVE_CN_CQL)

    override fun findBy(cpid: String): CnEntity? {
        val query = preparedFindByCpidCQL.bind()
            .apply {
                setString(columnCpid, cpid)
            }

        val resultSet = load(query)
        return resultSet.one()
            ?.let { convertToContractEntity(it) }
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

    override fun save(
        cn: CnEntity
    ): Boolean {
        val statements = preparedSaveCnIdCQL.bind()
            .apply {
                setString(columnCpid, cn.cpid)
                setString(columnOwner, cn.owner)
                setString(columnJsonData, cn.jsonData)
            }

        return saveCn(statements).wasApplied()
    }

    private fun saveCn(statement: BoundStatement): ResultSet = try {
        session.execute(statement)
    } catch (exception: Exception) {
        throw DatabaseInteractionException(message = "Error writing cancelled contract.", cause = exception)
    }
}
