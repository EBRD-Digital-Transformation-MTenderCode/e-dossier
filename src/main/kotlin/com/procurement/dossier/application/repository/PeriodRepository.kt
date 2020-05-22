package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.model.entity.PeriodEntity

interface PeriodRepository {
    fun findBy(cpid: Cpid, ocid: Ocid): PeriodEntity?
    fun saveNewPeriod(period: PeriodEntity): Boolean
    fun saveOrUpdatePeriod(period: PeriodEntity)
    fun tryFindBy(cpid: Cpid, ocid: Ocid): Result<PeriodEntity?, Fail.Incident>
}
