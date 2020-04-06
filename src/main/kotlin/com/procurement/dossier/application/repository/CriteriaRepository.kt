package com.procurement.dossier.application.repository

import com.procurement.dossier.application.model.entity.CnEntity
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result

interface CriteriaRepository {
    fun findBy(cpid: String): CnEntity?
    fun tryFindBy(cpid: String): Result<CnEntity?, Fail.Incident>
    fun save(cn: CnEntity): Boolean
    fun trySave(cn: CnEntity): Result<CnEntity, Fail.Incident>
    fun update(cn: CnEntity)
}