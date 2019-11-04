package com.procurement.procurer.application.service

import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.cn.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.cn.CheckResponsesRequest

interface JsonValidationService {
    fun validateCriteria(cm: CommandMessage): CheckCriteriaRequest
    fun validateResponses(cm: CommandMessage): CheckResponsesRequest
}