package com.procurement.dossier.application.repository

import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod

interface RulesRepository {
    fun findPeriodDurationBy(country: String, pmd: ProcurementMethod): Long?
}
