package com.procurement.procurer.infrastructure.generator

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.procurer.infrastructure.model.dto.bpe.ApiVersion
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandType
import com.procurement.procurer.infrastructure.model.dto.bpe.Context
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

object CommandMessageGenerator {
    const val COMMAND_ID = "COMMAND_ID"
    val COMMAND_VERSION: ApiVersion = ApiVersion.V_0_0_1

    fun generate(
        id: String = COMMAND_ID,
        version: ApiVersion = COMMAND_VERSION,
        command: CommandType,
        context: Context,
        data: JsonNode
    ): CommandMessage {
        return CommandMessage(
            id = id,
            version = version,
            command = command,
            context = context,
            data = data
        )
    }
}

object ContextGenerator {
    const val CPID = "cpid-1"
    val TOKEN: UUID = UUID.fromString("bd56490f-57ca-4d1a-9210-250cb9b4eed3")
    const val OWNER = "owner-1"
    const val COUNTRY = "MD"
    const val STAGE = "stage"
    const val PREV_STAGE = "prev_stage"
    const val START_DATE = "2011-06-05T17:59:00Z"

    fun generate(
        cpid: String? = CPID,
        ocid: String? = null,
        token: String? = TOKEN.toString(),
        owner: String? = OWNER,
        requestId: String? = null,
        operationId: String? = null,
        stage: String? = STAGE,
        prevStage: String? = PREV_STAGE,
        processType: String? = null,
        operationType: String? = null,
        phase: String? = null,
        country: String? = COUNTRY,
        language: String? = null,
        pmd: String? = null,
        startDate: String? = START_DATE,
        endDate: String? = null,
        id: String? = null
    ): Context {
        return Context(
            id = id,
            operationId = operationId,
            requestId = requestId,
            cpid = cpid,
            ocid = ocid,
            stage = stage,
            prevStage = prevStage,
            processType = processType,
            operationType = operationType,
            phase = phase,
            owner = owner,
            country = country,
            language = language,
            pmd = pmd,
            token = token,
            startDate = startDate,
            endDate = endDate
        )
    }
}

