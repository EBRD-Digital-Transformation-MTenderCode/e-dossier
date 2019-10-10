package com.procurement.procurer.infrastructure.bind.criteria

import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.nhaarman.mockito_kotlin.clearInvocations
import com.nhaarman.mockito_kotlin.mock
import com.procurement.procurer.application.repository.CriteriaRepository
import com.procurement.procurer.infrastructure.generator.CommandMessageGenerator
import com.procurement.procurer.infrastructure.generator.ContextGenerator
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandType
import com.procurement.procurer.infrastructure.model.dto.ocds.Operation
import com.procurement.procurer.infrastructure.model.dto.ocds.ProcurementMethod
import com.procurement.procurer.infrastructure.service.CriteriaService
import com.procurement.procurer.infrastructure.service.GenerationService
import com.procurement.procurer.json.loadJson
import com.procurement.procurer.json.toNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequirementDeserializerTest {

    private val generationService: GenerationService = mock()
    private val criteriaRepository: CriteriaRepository = mock()

    private val criteriaService: CriteriaService = CriteriaService(generationService, criteriaRepository)
    private val parseContext = JsonPath.using(Configuration.defaultConfiguration())

    private val CHECK_CRITERIA_REQUEST = "json/service/criteria/check/request/request_check_criteria_full.json"

    private val json = loadJson(CHECK_CRITERIA_REQUEST)

    @AfterEach
    fun clear() {
        clearInvocations(generationService)
    }

    @BeforeEach
    fun setup() {
//            testingBindingAndMapping<CheckCriteriaRequest>(CHECK_CRITERIA_REQUEST)
    }

    @Nested
    inner class ExpectedValue {

        @Test
        fun `boolean datatype - int value`() {
            val document = parseContext.parse(json)

            document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "expectedValue", 1)
            document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "boolean")

            val requestNode = document.jsonString().toNode()

            val cm = commandMessage(
                CommandType.CHECK_CRITERIA,
                data = requestNode
            )

            assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
        }

        @Test
        fun `string datatype -- int value`() {
            val document = parseContext.parse(json)

            document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "expectedValue", 1)
            document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "string")

            val requestNode = document.jsonString().toNode()

            val cm = commandMessage(
                CommandType.CHECK_CRITERIA,
                data = requestNode
            )

            assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
        }
    }

    @Nested
    inner class MinValue {

    }
}

fun commandMessage(
    command: CommandType,
    token: String = ContextGenerator.TOKEN.toString(),
    owner: String = ContextGenerator.OWNER,
    pmd: String = ProcurementMethod.OT.name,
    startDate: String = ContextGenerator.START_DATE,
    data: JsonNode
): CommandMessage {
    val context = ContextGenerator.generate(
        token = token,
        owner = owner,
        pmd = pmd,
        operationType = Operation.CREATE_CN_ON_PN.value,
        startDate = startDate
    )

    return CommandMessageGenerator.generate(
        command = command,
        context = context,
        data = data
    )
}
