package com.procurement.dossier.infrastructure.bind.criteria

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.nhaarman.mockito_kotlin.clearInvocations
import com.nhaarman.mockito_kotlin.mock
import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.application.service.Generable
import com.procurement.dossier.application.service.JsonValidationService
import com.procurement.dossier.infrastructure.config.ObjectMapperConfiguration
import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.CheckCriteriaRequest
import com.procurement.dossier.application.service.CriteriaService
import com.procurement.dossier.infrastructure.service.MedeiaValidationService
import com.procurement.dossier.json.exception.JsonBindingException
import com.procurement.dossier.json.loadJson
import com.procurement.dossier.json.toNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [ObjectMapperConfiguration::class])
class RequirementDeserializerTest: AbstractDTOTestBase<CheckCriteriaRequest>(
    CheckCriteriaRequest::class.java) {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val generationService: Generable = mock()
    private val criteriaRepository: CriteriaRepository = mock()

    private val CHECK_CRITERIA_REQUEST = "json/service/criteria/check/request/request_check_criteria_full.json"
    private val json = loadJson(CHECK_CRITERIA_REQUEST)
    private val parseContext = JsonPath.using(Configuration.defaultConfiguration())

    private lateinit var jsonValidationService: JsonValidationService
    private lateinit var criteriaService: CriteriaService

    @BeforeEach
    fun setup() {
        jsonValidationService = MedeiaValidationService(objectMapper)
        criteriaService = CriteriaService(
            generationService,
            criteriaRepository,
            jsonValidationService
        )
    }

    @AfterEach
    fun clear() {
        clearInvocations(generationService)
    }

    @Nested
    inner class ExpectedValue {

        @Test
        fun `boolean datatype - int value`() {
            val document = parseContext.parse(json)

            document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "expectedValue", 1)
            document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "boolean")

            val requestNode = document.jsonString().toNode()

            assertThrows<JsonBindingException> {   testBindingAndMapping(requestNode) }
        }

        @Test
        fun `string datatype -- int value`() {
            val document = parseContext.parse(json)

            document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "expectedValue", 1)
            document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "string")

            val requestNode = document.jsonString().toNode()

            assertThrows<JsonBindingException> { testBindingAndMapping(requestNode) }
        }
    }
}

