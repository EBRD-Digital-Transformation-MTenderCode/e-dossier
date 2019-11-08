package com.procurement.procurer.infrastructure.service

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.service.CriteriaService
import com.procurement.procurer.application.service.context.EvPanelsContext
import com.procurement.procurer.application.service.context.GetCriteriaContext
import com.procurement.procurer.infrastructure.converter.toResponseDto
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandType
import com.procurement.procurer.infrastructure.model.dto.bpe.ResponseDto
import com.procurement.procurer.infrastructure.model.dto.bpe.country
import com.procurement.procurer.infrastructure.model.dto.bpe.cpid
import com.procurement.procurer.infrastructure.model.dto.bpe.language
import com.procurement.procurer.infrastructure.model.dto.bpe.pmd
import com.procurement.procurer.infrastructure.model.dto.ocds.ProcurementMethod
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
            CommandType.CHECK_RESPONSES -> criteriaService.checkResponses(cm)
            CommandType.GET_CRITERIA -> {
                when (cm.pmd) {
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV -> {
                        val context = GetCriteriaContext(cpid = cm.cpid)
                        val serviceResponse = criteriaService.getCriteriaDetails(context = context)
                        val response = serviceResponse?.let { serviceResponse.toResponseDto() } ?: JsonNodeFactory.instance.objectNode()
                        ResponseDto(data = response)
                    }

                    ProcurementMethod.RT, ProcurementMethod.TEST_RT,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP -> {
                        throw ErrorException(ErrorType.INVALID_PMD)
                    }

                }

            }
            CommandType.CREATE_REQUESTS_FOR_EV_PANELS -> {
                when (cm.pmd) {
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV -> {
                        val context = EvPanelsContext(
                            cpid = cm.cpid,
                            country = cm.country,
                            language = cm.language,
                            pmd = cm.pmd
                        )
                        val serviceResponse = criteriaService.createRequestsForEvPanels(context = context)
                        val response = serviceResponse.toResponseDto()
                        ResponseDto(data = response)
                    }

                    ProcurementMethod.RT, ProcurementMethod.TEST_RT,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP -> {
                        throw ErrorException(ErrorType.INVALID_PMD)
                    }

                }

            }
        }

        return toObject(ResponseDto::class.java, toJson(response))
    }

}