package com.procurement.dossier.application.service

import com.procurement.dossier.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.dossier.infrastructure.model.dto.request.CheckCriteriaRequest
import com.procurement.dossier.infrastructure.model.dto.request.CheckResponsesRequest

interface JsonValidationService {
    fun validateCriteria(cm: CommandMessage): CheckCriteriaRequest
    fun validateResponses(cm: CommandMessage): CheckResponsesRequest
}