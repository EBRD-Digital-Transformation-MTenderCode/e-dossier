package com.procurement.dossier.application.repository.history

import com.procurement.dossier.infrastructure.model.entity.HistoryEntity

interface HistoryDao {
    fun getHistory(operationId: String, command: String): HistoryEntity?
    fun saveHistory(operationId: String, command: String, response: Any): HistoryEntity
}