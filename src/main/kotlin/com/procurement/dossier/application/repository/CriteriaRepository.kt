package com.procurement.dossier.application.repository

import com.procurement.dossier.application.model.entity.CnEntity

interface CriteriaRepository {
    fun findBy(cpid: String): CnEntity?
    fun save(cn: CnEntity): Boolean
}