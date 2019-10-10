package com.procurement.procurer.application.repository

import com.procurement.procurer.application.model.entity.CnEntity

interface CriteriaRepository {
    fun findBy(cpid: String): CnEntity?
    fun save(cn: CnEntity): Boolean
}