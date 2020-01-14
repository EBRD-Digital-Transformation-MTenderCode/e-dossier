package com.procurement.procurer.application.service

import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.request.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.request.CheckResponsesRequest

interface JsonValidationService {
    fun validateCriteria(cm: CommandMessage): CheckCriteriaRequest
    fun validateResponses(cm: CommandMessage): CheckResponsesRequest
}