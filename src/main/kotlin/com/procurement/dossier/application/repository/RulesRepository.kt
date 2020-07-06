package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import java.time.Duration

interface RulesRepository {
    fun findPeriodDuration(country: String, pmd: ProcurementMethod): Duration?
    fun findSubmissionsMinimumQuantity(country: String, pmd: ProcurementMethod): Result<Long?, Fail.Incident>
}
