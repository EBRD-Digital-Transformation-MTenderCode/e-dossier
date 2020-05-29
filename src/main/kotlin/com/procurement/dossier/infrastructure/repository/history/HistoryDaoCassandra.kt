package com.procurement.dossier.infrastructure.repository.history

import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.history.HistoryDao
import com.procurement.dossier.domain.util.extension.nowDefaultUTC
import com.procurement.dossier.domain.util.extension.toDate
import com.procurement.dossier.infrastructure.extension.cassandra.executeRead
import com.procurement.dossier.infrastructure.extension.cassandra.executeWrite
import com.procurement.dossier.infrastructure.model.entity.HistoryEntity
import com.procurement.dossier.infrastructure.utils.toJson
import org.springframework.stereotype.Repository

@Repository
class HistoryDaoCassandra(private val session: Session) : HistoryDao {

    companion object {
        private const val KEYSPACE = "dossier"
        private const val HISTORY_TABLE = "history"
        private const val OPERATION_ID = "command_id"
        private const val COMMAND = "command"
        private const val OPERATION_DATE = "command_date"
        private const val JSON_DATA = "json_data"

        private const val SAVE_HISTORY_CQL = """
               INSERT INTO $KEYSPACE.$HISTORY_TABLE(
                      $OPERATION_ID,
                      $COMMAND,
                      $OPERATION_DATE,
                      $JSON_DATA
               )
               VALUES(?, ?, ?, ?)
               IF NOT EXISTS
            """

        private const val FIND_HISTORY_ENTRY_CQL = """
               SELECT $OPERATION_ID,
                      $COMMAND,
                      $OPERATION_DATE,
                      $JSON_DATA
                 FROM $KEYSPACE.$HISTORY_TABLE
                WHERE $OPERATION_ID=?
                  AND $COMMAND=?
               LIMIT 1
            """
    }

    private val preparedSaveHistoryCQL = session.prepare(SAVE_HISTORY_CQL)
    private val preparedFindHistoryByCpidAndCommandCQL = session.prepare(FIND_HISTORY_ENTRY_CQL)

    override fun getHistory(operationId: String, command: String): HistoryEntity? {
        val query = preparedFindHistoryByCpidAndCommandCQL.bind()
            .apply {
                setString(OPERATION_ID, operationId)
                setString(COMMAND, command)
            }

        return query.executeRead(session, "Error reading from history")
            .one()
            ?.let { row ->
                HistoryEntity(
                    row.getString(OPERATION_ID),
                    row.getString(COMMAND),
                    row.getTimestamp(OPERATION_DATE),
                    row.getString(JSON_DATA)
                )
            }
    }

    override fun saveHistory(operationId: String, command: String, response: Any): HistoryEntity {
        val entity = HistoryEntity(
            operationId = operationId,
            command = command,
            operationDate = nowDefaultUTC().toDate(),
            jsonData = toJson(response)
        )

        val insert = preparedSaveHistoryCQL.bind()
            .apply {
                setString(OPERATION_ID, entity.operationId)
                setString(COMMAND, entity.command)
                setTimestamp(OPERATION_DATE, entity.operationDate)
                setString(JSON_DATA, entity.jsonData)
            }

        insert.executeWrite(session, "Error writing to history")

        return entity
    }
}
