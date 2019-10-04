package com.procurement.procurer.infrastructure.service

import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandType
import com.procurement.procurer.infrastructure.model.dto.bpe.ResponseDto
import com.procurement.procurer.infrastructure.utils.toJson
import com.procurement.procurer.infrastructure.utils.toObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandService(
    private val criteriaService: CriteriaService
) {
    companion object {
        private val log = LoggerFactory.getLogger(CommandService::class.java)
    }

    fun execute(cm: CommandMessage): ResponseDto {

        val response = when (cm.command) {

            CommandType.CHECK_CRITERIA  -> criteriaService.checkCriteria(cm)
            CommandType.CREATE_CRITERIA -> criteriaService.createCriteria(cm)
        }

        return toObject(ResponseDto::class.java, toJson(response))
    }

}