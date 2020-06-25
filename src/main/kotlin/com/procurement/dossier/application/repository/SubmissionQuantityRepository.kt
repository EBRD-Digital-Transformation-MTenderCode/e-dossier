package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod

interface SubmissionQuantityRepository {
    fun findMinimum(country: String, pmd: ProcurementMethod): Result<Long?, Fail.Incident>
}