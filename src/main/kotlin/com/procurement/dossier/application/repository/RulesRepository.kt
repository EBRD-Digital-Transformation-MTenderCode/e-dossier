package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.model.dto.ocds.Operation
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import java.time.Duration

interface RulesRepository {
    fun findExtensionAfterUnsuspended(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation? = null
    ): Duration?

    fun findPeriodDuration(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation? = null
    ): Duration?

    fun findSubmissionsMinimumQuantity(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation
    ): Result<Long?, Fail.Incident.Database>

    fun findSubmissionValidState(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation
    ): Result<SubmissionStatus?, Fail.Incident.Database>
}
