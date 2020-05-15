package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.infrastructure.model.entity.PeriodEntity

interface PeriodRepository {
    fun findBy(cpid: Cpid, ocid: Ocid): PeriodEntity?
    fun saveNewPeriod(period: PeriodEntity): Boolean
    fun saveOrUpdatePeriod(period: PeriodEntity)
}
