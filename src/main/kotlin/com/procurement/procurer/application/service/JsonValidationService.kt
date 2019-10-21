package com.procurement.procurer.application.service

import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.cn.CheckCriteriaRequest

interface JsonValidationService {
    fun validateViaJsonSchema(cm: CommandMessage): CheckCriteriaRequest
}