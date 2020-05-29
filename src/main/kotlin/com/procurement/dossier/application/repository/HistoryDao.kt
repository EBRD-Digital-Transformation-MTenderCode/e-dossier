package com.procurement.dossier.application.repository

import com.procurement.dossier.infrastructure.model.entity.HistoryEntity

interface HistoryDao {
    fun getHistory(operationId: String, command: String): HistoryEntity?
    fun saveHistory(operationId: String, command: String, response: Any): HistoryEntity
}