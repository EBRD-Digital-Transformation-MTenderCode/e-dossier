package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod

interface RulesRepository {
    fun findPeriodDuration(country: String, pmd: ProcurementMethod): Long?
    fun findSubmissionsMinimumQuantity(country: String, pmd: ProcurementMethod): Result<Long?, Fail.Incident>
}
