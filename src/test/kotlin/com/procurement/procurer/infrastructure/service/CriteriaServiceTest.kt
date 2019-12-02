package com.procurement.procurer.infrastructure.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.clearInvocations
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.model.data.CheckCriteriaData.Tender.Criteria.RequirementGroup
import com.procurement.procurer.application.model.data.CheckCriteriaData.Tender.Item
import com.procurement.procurer.application.model.data.Requirement
import com.procurement.procurer.application.model.entity.CnEntity
import com.procurement.procurer.application.repository.CriteriaRepository
import com.procurement.procurer.application.service.CriteriaService
import com.procurement.procurer.application.service.Generable
import com.procurement.procurer.application.service.JsonValidationService
import com.procurement.procurer.application.service.context.CheckResponsesContext
import com.procurement.procurer.application.service.context.CreateCriteriaContext
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.procurer.infrastructure.config.ObjectMapperConfiguration
import com.procurement.procurer.infrastructure.generator.CommandMessageGenerator
import com.procurement.procurer.infrastructure.generator.ContextGenerator
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandType
import com.procurement.procurer.infrastructure.model.dto.request.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.request.CheckResponsesRequest
import com.procurement.procurer.infrastructure.model.dto.request.CreateCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.response.CreateCriteriaResponse
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.model.dto.ocds.ConversionsRelatesTo
import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaSource
import com.procurement.procurer.infrastructure.model.dto.ocds.Operation
import com.procurement.procurer.infrastructure.model.dto.ocds.ProcurementMethod
import com.procurement.procurer.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.procurer.json.getArray
import com.procurement.procurer.json.getObject
import com.procurement.procurer.json.loadJson
import com.procurement.procurer.json.putAttribute
import com.procurement.procurer.json.putObject
import com.procurement.procurer.json.testingBindingAndMapping
import com.procurement.procurer.json.toJson
import com.procurement.procurer.json.toNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [ObjectMapperConfiguration::class])
class CriteriaServiceTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val generationService: Generable = mock()
    private val criteriaRepository: CriteriaRepository = mock()

    private lateinit var criteriaService: CriteriaService
    private val parseContext = JsonPath.using(Configuration.defaultConfiguration())

    private lateinit var jsonValidationService: JsonValidationService

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
    inner class CheckCriteria {

        private val CHECK_CRITERIA_REQUEST = "json/service/criteria/check/request/request_check_criteria_full.json"
        private val json = loadJson(CHECK_CRITERIA_REQUEST)

        @BeforeEach
        fun setup() {
            testingBindingAndMapping<CheckCriteriaRequest>(CHECK_CRITERIA_REQUEST)
        }

        @Nested
        inner class FReq_1_1_1_22 {

            @ParameterizedTest(name = "awardCriteria = {0} without awardCriteriaDetails ")
            @CsvSource(
                "costOnly",
                "ratedCriteria"
            )
            fun `Non price awardCriteria without awardCriteriaDetails `(
                awardCriteria: String
            ) {

                val requestNode = json.toNode()

                requestNode.getObject("tender")
                    .put("awardCriteria", AwardCriteria.fromString(awardCriteria).value)
                    .remove("awardCriteriaDetails")

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_AWARD_CRITERIA, exception.error)
            }

            @ParameterizedTest(name = "awardCriteria = {0}, without awardCriteriaDetails")
            @CsvSource(
                "priceOnly"
            )
            fun `price awardCriteria without awardCriteriaDetails `(
                awardCriteria: String
            ) {

                val requestNode = json.toNode()
                val tenderNode = requestNode.getObject("tender")
                val criteria = tenderNode.get("criteria").get(0) as ObjectNode
                val tenderCriteria = criteria.deepCopy()
                tenderCriteria.putAttribute("relatesTo", "tenderer")

                tenderNode.put("awardCriteria", AwardCriteria.fromString(awardCriteria).value)
                    .remove("awardCriteriaDetails")

                tenderNode.remove("conversions")
                tenderNode.putArray("criteria")
                    .add(tenderCriteria)

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )
                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @ParameterizedTest(name = "awardCriteria = {0},  awardCriteriaDetails = {1} ")
            @CsvSource(
                "priceOnly, manual",
                "priceOnly, automated"
            )
            fun `price awardCriteria without awardCriteriaDetails d`(
                awardCriteria: String,
                awardCriteriaDetails: String
            ) {

                val requestNode = json.toNode()

                val tenderNode = requestNode.getObject("tender")
                val criteria = tenderNode.get("criteria").get(0) as ObjectNode
                val tenderCriteria = criteria.deepCopy()
                tenderCriteria.putAttribute("relatesTo", "tenderer")

                tenderNode.put("awardCriteria", AwardCriteria.fromString(awardCriteria).value)
                    .put("awardCriteriaDetails", AwardCriteriaDetails.fromString(awardCriteriaDetails).value)
                    .remove("conversions")
                tenderNode.putArray("criteria")
                    .add(tenderCriteria)


                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }
        }

        @Nested
        inner class FReq_1_1_1_23 {

            @ParameterizedTest(name = "awardCriteria = {0},  awardCriteriaDetails = {1}, wothout [2] ")
            @CsvSource(
                "costOnly, automated, conversions",
                "qualityOnly, automated, conversions",
                "ratedCriteria, automated, conversions",
                "costOnly, automated, criteria",
                "qualityOnly, automated, criteria",
                "ratedCriteria, automated, criteria"
            )
            fun `non price awardCriteria with awardCriteriaDetails='automated' | No conversion`(
                awardCriteria: String,
                awardCriteriaDetails: String,
                fieldname: String
            ) {

                val requestNode = json.toNode()
                requestNode.getObject("tender")
                    .put("awardCriteria", AwardCriteria.fromString(awardCriteria).value)
                    .put("awardCriteriaDetails", AwardCriteriaDetails.fromString(awardCriteriaDetails).value)
                    .remove(fieldname)

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }

                if (fieldname == "criteria") assertEquals(ErrorType.INVALID_CONVERSION, exception.error)
                if (fieldname == "conversions") assertEquals(ErrorType.INVALID_AWARD_CRITERIA, exception.error)
            }

            @ParameterizedTest(name = "awardCriteria = {0},  awardCriteriaDetails = {1} ")
            @CsvSource(
                "costOnly, automated",
                "qualityOnly, automated",
                "ratedCriteria, automated"
            )
            fun `price awardCriteria with awardCriteriaDetails='automated' | No Criteria No Conversion`(
                awardCriteria: String,
                awardCriteriaDetails: String
            ) {

                val requestNode = json.toNode()
                requestNode.getObject("tender")
                    .put("awardCriteria", AwardCriteria.fromString(awardCriteria).value)
                    .put("awardCriteriaDetails", AwardCriteriaDetails.fromString(awardCriteriaDetails).value)
                    .remove(listOf("criteria", "conversions"))

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_AWARD_CRITERIA, exception.error)
            }
        }

        @Nested
        inner class FReq_1_1_1_3 {

            @Test
            fun `Invalid Item id in criteria`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].relatedItem", "UNKNOWN_ID")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }

            @Test
            fun `Invalid Lot id in criteria`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].relatesTo", "lot")
                document.set("$.tender.criteria[2].relatedItem", "UNKNOWN_ID")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }
        }

        @Nested
        inner class FReq_1_1_1_4 {

            @Test
            fun `Expected value with minValue || maxValue`() {

                val document = parseContext.parse(json)
                document.put("$.tender.criteria[1].requirementGroups[0].requirements[1]", "expectedValue", 2.0)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue", 1.0)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue", 3.0)

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `Expected value == number || DataType == boolean`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "number")
                document.put("$.tender.criteria[1].requirementGroups[0].requirements[1]", "expectedValue", true)
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue")
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue")

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `Expected value == number || DataType == integer`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "integer")
                document.put("$.tender.criteria[1].requirementGroups[0].requirements[1]", "expectedValue", 1.23)
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue")
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue")

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Nested
            inner class RangeValue {

                @Test
                fun `RangeValue Integer`() {
                    val document = parseContext.parse(json)

                    document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "minValue", 1)
                    document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "maxValue", 30)
                    document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "integer")

                    val requestNode = document.jsonString().toNode()

                    val requirement = requestNode
                        .getObject("tender")
                        .getArray("criteria")[3]
                        .getArray("requirementGroups")[0]
                        .getArray("requirements")[0]

                    (requirement as ObjectNode).remove("expectedValue")

                    val cm = commandMessage(
                        CommandType.CHECK_CRITERIA,
                        data = requestNode
                    )

                    assertDoesNotThrow { criteriaService.checkCriteria(cm) }
                }
            }

            @Nested
            inner class MinValue {

                @Test
                fun `MinValue Integer`() {
                    val document = parseContext.parse(json)

                    document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "minValue", 1)
                    document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "integer")

                    val requestNode = document.jsonString().toNode()

                    val requirement = requestNode
                        .getObject("tender")
                        .getArray("criteria")[3]
                        .getArray("requirementGroups")[0]
                        .getArray("requirements")[0]

                    (requirement as ObjectNode).remove("expectedValue")

                    val cm = commandMessage(
                        CommandType.CHECK_CRITERIA,
                        data = requestNode
                    )

                    assertDoesNotThrow { criteriaService.checkCriteria(cm) }
                }

                @Test
                fun `MinValue Number`() {
                    val document = parseContext.parse(json)

                    document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "minValue", 1.0)
                    document.put("$.tender.conversions[3].coefficients[0]", "value", 1.0)
                    document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "number")

                    val requestNode = document.jsonString().toNode()

                    val requirement = requestNode
                        .getObject("tender")
                        .getArray("criteria")[3]
                        .getArray("requirementGroups")[0]
                        .getArray("requirements")[0]

                    (requirement as ObjectNode).remove("expectedValue")

                    val cm = commandMessage(
                        CommandType.CHECK_CRITERIA,
                        data = requestNode
                    )

                    assertDoesNotThrow { criteriaService.checkCriteria(cm) }
                }
            }

            @Nested
            inner class MaxValue {

                @Test
                fun `MaxValue Integer`() {
                    val document = parseContext.parse(json)

                    document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "maxValue", 40)
                    document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "integer")

                    val requestNode = document.jsonString().toNode()

                    val requirement = requestNode
                        .getObject("tender")
                        .getArray("criteria")[3]
                        .getArray("requirementGroups")[0]
                        .getArray("requirements")[0]

                    (requirement as ObjectNode).remove("expectedValue")

                    val cm = commandMessage(
                        CommandType.CHECK_CRITERIA,
                        data = requestNode
                    )

                    assertDoesNotThrow { criteriaService.checkCriteria(cm) }
                }

                @Test
                fun `MaxValue Number`() {
                    val document = parseContext.parse(json)

                    document.put("$.tender.criteria[3].requirementGroups[0].requirements[0]", "maxValue", 4.0)
                    document.put("$.tender.conversions[3].coefficients[0]", "value", 4.0)
                    document.set("$.tender.criteria[3].requirementGroups[0].requirements[0].dataType", "number")

                    val requestNode = document.jsonString().toNode()

                    val requirement = requestNode
                        .getObject("tender")
                        .getArray("criteria")[3]
                        .getArray("requirementGroups")[0]
                        .getArray("requirements")[0]

                    (requirement as ObjectNode).remove("expectedValue")

                    val cm = commandMessage(
                        CommandType.CHECK_CRITERIA,
                        data = requestNode
                    )

                    assertDoesNotThrow { criteriaService.checkCriteria(cm) }
                }
            }
        }

        @Nested
        inner class FReq_1_1_1_5 {

            @Test
            fun `MinValue less than MaxValue`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "number")
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue", 0.02)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue", 234.14)

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `MinValue greater than MaxValue`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "number")
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue", 234.14)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue", 123.0)

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_REQUIREMENT_VALUE, exception.error)
            }

            @Test
            fun `MinValue == MaxValue`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "number")
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue", 0.14)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue", 234.14)

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }
        }

        @Nested
        inner class FReq_1_1_1_6 {

            @Test
            fun `startDate greater than endDate`() {
                val startDate = "2019-09-24T12:16:21Z"
                val endDate = "2019-09-15T14:33:51Z"

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].period.startDate", startDate)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].period.endDate", endDate)

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_PERIOD_VALUE, exception.error)
            }

            @Test
            fun `startDate == endDate`() {
                val startDate = "2019-09-24T12:16:21Z"
                val endDate = "2019-09-24T12:16:21Z"

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].period.startDate", startDate)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].period.endDate", endDate)

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `startDate less than endDate`() {
                val startDate = "2019-09-11T12:16:21Z"
                val endDate = "2019-09-24T12:16:21Z"

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].period.startDate", startDate)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].period.endDate", endDate)

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }
        }

        @Nested
        inner class FReq_1_1_1_8 {

            @Test
            fun `Invalid criteria_relatesTo value`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].relatesTo", "INVALID_RELATES_TO_VALUE")

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `Unique criteria id in array`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[0].id", "111")
                document.set("$.tender.criteria[1].id", "222")
                document.set("$.tender.criteria[2].id", "333")

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `Not unique criteria id in array`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[0].id", "111")
                document.set("$.tender.criteria[1].id", "111")
                document.set("$.tender.criteria[2].id", "333")

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }

            @Test
            fun `No requirement group in criteria`() {
                val document = parseContext.parse(json)
                document.delete("$.tender.criteria[2].requirementGroups")
                document.put("$.tender.criteria[2]", "requirementGroups", emptyList<RequirementGroup>())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `Al least one requirement group in criteria`() {
                val document = parseContext.parse(json)

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `Unique requirement group id  in criteria`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].requirementGroups[0].id", "111")
                document.set("$.tender.criteria[2].requirementGroups[1].id", "111")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }

            @Test
            fun `Not unique requirement group id  in criteria`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].requirementGroups[0].id", "111")
                document.set("$.tender.criteria[2].requirementGroups[1].id", "222")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `No requirements in requirement group`() {
                val document = parseContext.parse(json)
                document.delete("$.tender.criteria[2].requirementGroups[0].requirements")
                document.put("$.tender.criteria[2].requirementGroups[0]", "requirements", emptyList<Requirement>())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `At least one requirements in requirement group`() {
                val document = parseContext.parse(json)
                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `Same requirements id in different requirements object (one requirement group)`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", "111")
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[1].id", "111")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }

            @Test
            fun `Same requirements id in different requirementGroup`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", "111")
                document.set("$.tender.criteria[2].requirementGroups[1].requirements[0].id", "111")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }

            @Test
            fun `Same requirements id in different criteria`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[0].id", "111")
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", "111")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }
        }

        @Nested
        inner class FReq_1_1_1_9 {

            @Test
            fun `1 requirement, 2 conversion`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[0].id", "111")
                document.set("$.tender.conversions[0].relatedItem", "111")
                document.set("$.tender.conversions[1].relatedItem", "111")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CONVERSION, exception.error)
            }
        }

        @Nested
        inner class FReq_1_1_1_10 {

            @Test
            fun `Invalid relatedItem in Conversion`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[0].id", "111")
                document.set("$.tender.conversions[0].relatedItem", "INVALID_ID")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CONVERSION, exception.error)
            }
        }

        @Nested
        inner class FReq_1_1_1_11 {

            private val COEFFICIENT_MAX_VALUE = BigDecimal(1)
            private val COEFFICIENT_NORNAL_VALUE = BigDecimal(0.5)

            @Test
            fun `Conversion coefficient less then MIN`() {
                val document = parseContext.parse(json)
                document.set("$.tender.conversions[0].coefficients[0].coefficient", BigDecimal(0.001))

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CONVERSION, exception.error)
            }

            @Test
            fun `Conversion coefficient ==  MIN`() {
                val document = parseContext.parse(json)
                document.set("$.tender.conversions[0].coefficients[0].coefficient", BigDecimal(0.010000))

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `Conversion coefficient greater than  MAX`() {
                val document = parseContext.parse(json)
                document.set("$.tender.conversions[0].coefficients[0].coefficient", 2.3.toBigDecimal())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CONVERSION, exception.error)
            }

            @Test
            fun `Conversion coefficient ==  MAX`() {
                val document = parseContext.parse(json)
                document.set("$.tender.conversions[0].coefficients[0].coefficient", COEFFICIENT_MAX_VALUE)

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `coefficient less than MAX, greater than MIN`() {
                val document = parseContext.parse(json)
                document.set("$.tender.conversions[0].coefficients[0].coefficient", (0.5).toBigDecimal())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }
        }

        @Nested
        inner class FReq_1_1_1_12 {

            private val UNIQUE_ID = "003-1-1"

            @Test
            fun `Conversion coefficient == boolean, Requirement value == integer `() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", UNIQUE_ID)
                document.set("$.tender.conversions[1].relatedItem", UNIQUE_ID)
                document.set("$.tender.conversions[1].coefficients[0].value", true)

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CONVERSION, exception.error)
            }

            @Test
            fun `Conversion coefficient == number, Requirement value == integer `() {

                val document = parseContext.parse(json)
                val expectedValue = document.read<Int>("$.tender.criteria[2].requirementGroups[0].requirements[0].expectedValue")
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", UNIQUE_ID)
                document.set("$.tender.conversions[1].relatedItem", UNIQUE_ID)
                document.set("$.tender.conversions[1].coefficients[0].value", expectedValue)

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `Conversion coefficient == number, Requirement value == number`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", UNIQUE_ID)
                document.set(
                    "$.tender.criteria[2].requirementGroups[0].requirements[0].dataType",
                    RequirementDataType.NUMBER.value()
                )
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].expectedValue", .50)
                document.set("$.tender.conversions[1].relatedItem", UNIQUE_ID)
                document.set("$.tender.conversions[1].coefficients[0].value", 0.5.toBigDecimal())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }
        }

        @Nested
        inner class FReq_1_1_1_13 {

            @Test
            fun `Valid cast coefficient`() {
                val document = parseContext.parse(json)

//                val tenderConversionCoefficients = document.read<List<BigDecimal>>("$.tender.conversions[2].coefficients[*].coefficient")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `Invalid cast coefficient`() {
                val document = parseContext.parse(json)

                document.set("$.tender.conversions[4].coefficients[0].coefficient", 0.1)

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
            }
        }

        @Nested
        inner class FReq_1_1_1_14 {

            @Test
            fun `Invalid relatesTo in conversion`() {
                val document = parseContext.parse(json)
                document.set("$.tender.conversions[1].relatesTo", "INVALID_ENUM")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }
        }

        @Nested
        inner class FReq_1_1_1_15 {

            @Test
            fun `Invalid awardCriteria`() {
                val document = parseContext.parse(json)
                document.set("$.tender.awardCriteria", "INVALID_ENUM")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `Invalid awardCriteriaDetails`() {
                val document = parseContext.parse(json)
                document.set("$.tender.awardCriteriaDetails", "INVALID_ENUM")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }
        }

        @Nested
        inner class FReq_1_1_1_16 {

            @Test
            fun `empty items`() {
                val document = parseContext.parse(json)
                document.put("$", "items", emptyList<Item>())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `empty requirement group`() {
                val document = parseContext.parse(json)
                document.put("$.tender.criteria[0]", "requirementGroups", emptyList<RequirementGroup>())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }

            @Test
            fun `same id in requirements`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[0].id", "111")
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].id", "111")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
            }
        }

        @Nested
        inner class Exception {

            @Test
            fun `coefficient value exeption`() {
                val document = parseContext.parse(json)
                document.set("$.tender.conversions[0].coefficients[0].value", emptyArray<String>())
                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_JSON, exception.error)
            }
        }
    }

    @Nested
    inner class CreateCriteria {

        private val CREATE_CRITERIA_REQUEST = "json/service/criteria/create/request/request_create_criteria_full.json"
        private val CREATE_CRITERIA_RESPONSE = "json/service/criteria/create/response/response_create_criteria_full.json"

        private val json = loadJson(CREATE_CRITERIA_REQUEST)

        @BeforeEach
        fun setup() {
            testingBindingAndMapping<CreateCriteriaRequest>(CREATE_CRITERIA_REQUEST)
            testingBindingAndMapping<CreateCriteriaResponse>(CREATE_CRITERIA_RESPONSE)
        }

        @Test
        fun `full flow`() {

            val expectedCriteriaIds = listOf(
                UUID.fromString("112315a4-dc87-426f-8e8a-142b7eec1001"),
                UUID.fromString("112315a4-dc87-426f-8e8a-142b7eec2002"),
                UUID.fromString("112315a4-dc87-426f-8e8a-142b7eec3003")
            )

            val expectedRgIds = listOf(
                UUID.fromString("d6ed4efe-4aa4-42c4-9324-eed808d22dd1"),
                UUID.fromString("d6ed4efe-4aa4-42c4-9324-eed808d22dd2"),
                UUID.fromString("d6ed4efe-4aa4-42c4-9324-eed808d22dd3")
            )

            val expectedRequirementIds = listOf(
                UUID.fromString("1a251e1d-1d6a-4944-a3ad-c2436eb2de51"),
                UUID.fromString("1a251e1d-1d6a-4944-a3ad-c2436eb2de52"),
                UUID.fromString("1a251e1d-1d6a-4944-a3ad-c2436eb2de53"),
                UUID.fromString("1a251e1d-1d6a-4944-a3ad-c2436eb2de54"),
                UUID.fromString("1a251e1d-1d6a-4944-a3ad-c2436eb2de55")
            )

            val expectedConversionIds = listOf(
                UUID.fromString("8f6c268a-b1e4-4c12-a218-144dd1ba3881"),
                UUID.fromString("8f6c268a-b1e4-4c12-a218-144dd1ba3882")
            )


            val expectedCoefficientIds = listOf(
                UUID.fromString("099bc62e-2192-4c45-b5ed-8a840f9955b1"),
                UUID.fromString("099bc62e-2192-4c45-b5ed-8a840f9955b2"),
                UUID.fromString("099bc62e-2192-4c45-b5ed-8a840f9955b3"),
                UUID.fromString("099bc62e-2192-4c45-b5ed-8a840f9955b4")
            )

            whenever(generationService.generatePermanentCriteriaId())
                .thenReturn(expectedCriteriaIds[0], expectedCriteriaIds[1], expectedCriteriaIds[2])

            whenever(generationService.generatePermanentRequirementGroupId())
                .thenReturn(expectedRgIds[0], expectedRgIds[1], expectedRgIds[2])

            whenever(generationService.generatePermanentRequirementId())
                .thenReturn(
                    expectedRequirementIds[0],
                    expectedRequirementIds[1],
                    expectedRequirementIds[2],
                    expectedRequirementIds[3],
                    expectedRequirementIds[4]
                )

            whenever(generationService.generatePermanentConversionId())
                .thenReturn(expectedConversionIds[0], expectedConversionIds[1])

            whenever(generationService.generatePermanentCoefficientId())
                .thenReturn(
                    expectedCoefficientIds[0],
                    expectedCoefficientIds[1],
                    expectedCoefficientIds[2],
                    expectedCoefficientIds[3]
                )

            whenever(criteriaRepository.save(any()))
                .thenReturn(true)

            val document = parseContext.parse(json)

            val requestNode = document.jsonString().toNode()

            requestNode.getObject("tender")
                .put("awardCriteria", "priceOnly")
                .put("awardCriteriaDetails", "manual")
                .remove("criteria[0].relatesTo")

            val cm = commandMessage(
                CommandType.CREATE_CRITERIA,
                data = requestNode
            )

            println(cm.toJson())

            val context = CreateCriteriaContext(cpid = "cpid", owner = "owner")
            val response = criteriaService.createCriteria(cm, context = context)

            verify(criteriaRepository, times(1))
                .save(any())

            val responseDto = response

            val criteriaId = responseDto.criteria!!.map { UUID.fromString(it.id) }
            assertEquals(expectedCriteriaIds, criteriaId)

            val requirementGroupsId = responseDto.criteria!!.flatMap { it.requirementGroups }
                .map { UUID.fromString(it.id) }
            assertEquals(expectedRgIds, requirementGroupsId)

            val requirementsId = responseDto.criteria!!
                .flatMap { it.requirementGroups }
                .flatMap { it.requirements }
                .map { UUID.fromString(it.id) }

            assertEquals(expectedRequirementIds, requirementsId)

            val conversionsId = responseDto.conversions!!.map { UUID.fromString(it.id) }
            assertEquals(expectedConversionIds, conversionsId)

            val coefficientId = responseDto.conversions!!.flatMap { it.coefficients }
                .map { UUID.fromString(it.id) }
            assertEquals(expectedCoefficientIds, coefficientId)

            val criteriaSource = responseDto.criteria!![0].source
            assertNotNull(criteriaSource)
            assertEquals(CriteriaSource.TENDERER, criteriaSource)

            val awardCriteriaDeatils = responseDto.awardCriteriaDetails
            assertNotNull(awardCriteriaDeatils)
            assertEquals(AwardCriteriaDetails.AUTOMATED, awardCriteriaDeatils)

            val conversionRelations = responseDto.conversions!!
                .filter { it.relatesTo == ConversionsRelatesTo.REQUIREMENT }
                .map { it.relatedItem }

            conversionRelations.forEach {
                assertTrue(expectedRequirementIds.contains(UUID.fromString(it)))
            }
        }
    }

    @Nested
    inner class CheckResponses {
        private val CPID = "cpid-1"

        private val CHECK_RESPONSES_REQUEST = "json/service/criteria/response/check/request/request_check_responses_full.json"
        private val CHECK_RESPONSES_DB = "json/service/criteria/response/check/check_responses_entity.json"
        private val json = loadJson(CHECK_RESPONSES_REQUEST)
        private val entity = loadJson(CHECK_RESPONSES_DB)

        @BeforeEach
        fun setup() {
            testingBindingAndMapping<CheckResponsesRequest>(CHECK_RESPONSES_REQUEST)
        }

        @AfterEach
        fun clear() {
            clearInvocations(criteriaRepository)
        }

        @Test
        fun `successful | responses passed`() {
            val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)

            whenever(criteriaRepository.findBy(any()))
                .thenReturn(cnEntity)

            val requestNode = (json.toNode() as ObjectNode)
            val bidNode = requestNode.get("bid") as ObjectNode

            val requirementResponse = (bidNode.get("requirementResponses") as ArrayNode).get(1)

            bidNode.putArray("requirementResponses")
                .add(requirementResponse)

            val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
            val cm = commandMessage(command = CommandType.CHECK_RESPONSES, data = requestNode)

            assertDoesNotThrow { criteriaService.checkResponses(cm, context = context) }
        }

        @Test
        fun `successful | responses not passsed`() {
            val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)

            whenever(criteriaRepository.findBy(any()))
                .thenReturn(cnEntity)

            val requestNode = json.toNode()
            requestNode.getObject("bid")
                .remove("requirementResponses")

            val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
            val cm = commandMessage(command = CommandType.CHECK_RESPONSES, data = requestNode)

            val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
            assertEquals(ErrorType.INVALID_REQUIREMENT_RESPONSE, exception.error)
        }

        @Nested
        inner class FReq_1_2_1_1 {

            private val INVALID_REQUIREMENT_ID = "UNKNOWN_ID"

            @Test
            fun `Passed requirement that doesn't exists`() {
                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val requestNode = json.toNode()

                requestNode.getObject("bid")
                    .getArray("requirementResponses")
                    .get(0)
                    .getObject("requirement")
                    .put("id", INVALID_REQUIREMENT_ID)

                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_REQUIREMENT_VALUE, exception.error)
            }
        }

        private fun generateLotCriteria(requestNode: JsonNode, entityNode: JsonNode): JsonNode {
            val relatedLot = requestNode.getObject("bid").getArray("relatedLots").get(0).asText()
            return entityNode.getArray("criteria").get(1).toObjectNode()
                .putAttribute("relatesTo", "lot")
                .putAttribute("relatedItem", relatedLot)
        }

        private fun getLotCriteriaRequirementId(node: JsonNode): String {
            return node.getArray("requirementGroups").get(0).toObjectNode()
                .getArray("requirements").get(0).toObjectNode()
                .get("id").asText()
        }

        private fun generateItemCriteria(requestNode: JsonNode, entityNode: JsonNode): JsonNode {
            val relatedItem = requestNode.getArray("items").get(0).get("id").asText()
            return entityNode.getArray("criteria").get(2).toObjectNode()
                .putAttribute("relatesTo", "item")
                .putAttribute("relatedItem", relatedItem)
        }

        private fun getItemCriteriaRequirementId(node: JsonNode): String {
            return node.getArray("requirementGroups").get(0).toObjectNode()
                .getArray("requirements").get(0).toObjectNode()
                .get("id").asText()
        }


        private fun getSampleRequirementResponse(requestNode: JsonNode): ObjectNode {
            return requestNode.getObject("bid")
                .getArray("requirementResponses").get(0).toObjectNode()
        }

        @Nested
        inner class FReq_1_2_1_2 {

            @Test
            fun `Not answer on item requirement`() {
                val FIRST_ID = "first-requirement-response-id"
                val SECOND_ID = "second-requirement-response-id"

                val entityNode = entity.toNode()
                val requestNode = json.toNode()

                val lotCriteria = generateLotCriteria(requestNode = requestNode, entityNode = entityNode)
                val lotRequirementId = getLotCriteriaRequirementId(lotCriteria)

                generateItemCriteria(requestNode = requestNode, entityNode = entityNode)

                val sampleRequirementResponse = getSampleRequirementResponse(requestNode = requestNode)

                val lotRequirementResponse = sampleRequirementResponse.deepCopy()
                lotRequirementResponse.putAttribute("value", 23.56)
                    .putAttribute("id", SECOND_ID)
                    .getObject("requirement")
                    .putAttribute("id", lotRequirementId)

                val tenderRequirementResponse = requestNode.getObject("bid")
                    .getArray("requirementResponses").get(1).toObjectNode()
                    .putAttribute("id", FIRST_ID)

                requestNode.getObject("bid")
                    .putArray("requirementResponses")
                    .putObject(tenderRequirementResponse)
                    .putObject(lotRequirementResponse)

                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entityNode.toJson())
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_REQUIREMENT_VALUE, exception.error)
            }

            @Test
            fun `Not answer on tender requirement`() {
                val FIRST_ID = "first-requirement-response-id"
                val SECOND_ID = "second-requirement-response-id"

                val entityNode = entity.toNode()
                val requestNode = json.toNode()

                val lotCriteria = generateLotCriteria(requestNode = requestNode, entityNode = entityNode)
                val lotRequirementId = getLotCriteriaRequirementId(lotCriteria)

                val itemCriteria = generateItemCriteria(requestNode = requestNode, entityNode = entityNode)
                val itemRequirementId = getItemCriteriaRequirementId(itemCriteria)

                val sampleRequirementResponse = getSampleRequirementResponse(requestNode = requestNode)

                val lotRequirementResponse = sampleRequirementResponse.deepCopy()
                lotRequirementResponse.putAttribute("value", 23.56)
                    .putAttribute("id", FIRST_ID)
                    .getObject("requirement")
                    .putAttribute("id", lotRequirementId)

                val itemRequirementResponse = sampleRequirementResponse.deepCopy()
                itemRequirementResponse.putAttribute("value", "SAMPLE_ANSWER")
                    .putAttribute("id", SECOND_ID)
                    .getObject("requirement")
                    .putAttribute("id", itemRequirementId)

                requestNode.getObject("bid")
                    .putArray("requirementResponses")
                    .putObject(lotRequirementResponse)
                    .putObject(itemRequirementResponse)

                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_REQUIREMENT_VALUE, exception.error)
            }
        }

        @Nested
        inner class FReq_1_2_1_3 {

            @Test
            fun `duplicated answer`() {
                val FIRST_ID = "first-requirement-response-id"
                val SECOND_ID = "second-requirement-response-id"
                val THIRD_ID = "third-requirement-response-id"

                val entityNode = entity.toNode()
                val requestNode = json.toNode()

                val lotCriteria = generateLotCriteria(requestNode = requestNode, entityNode = entityNode)
                val lotRequirementId = getLotCriteriaRequirementId(lotCriteria)

                val itemCriteria = generateItemCriteria(requestNode = requestNode, entityNode = entityNode)
                val itemRequirementId = getItemCriteriaRequirementId(itemCriteria)

                val sampleRequirementResponse = getSampleRequirementResponse(requestNode = requestNode)

                val lotRequirementResponse = sampleRequirementResponse.deepCopy()
                lotRequirementResponse.putAttribute("value", 23.56)
                    .putAttribute("id", THIRD_ID)
                    .getObject("requirement")
                    .putAttribute("id", lotRequirementId)

                val itemRequirementResponse = sampleRequirementResponse.deepCopy()
                itemRequirementResponse.putAttribute("value", "SAMPLE_ANSWER")
                    .putAttribute("id", SECOND_ID)
                    .getObject("requirement")
                    .putAttribute("id", itemRequirementId)

                val tenderRequirementResponse = requestNode.getObject("bid")
                    .getArray("requirementResponses").get(1).toObjectNode()
                    .putAttribute("id", FIRST_ID)

                val duplicatedTenderRequirementResponse = tenderRequirementResponse.deepCopy()
                    .putAttribute("id" , "ANOTHER_ID")

                requestNode.getObject("bid")
                    .putArray("requirementResponses")
                    .putObject(tenderRequirementResponse)
                    .putObject(duplicatedTenderRequirementResponse)
                    .putObject(lotRequirementResponse)
                    .putObject(itemRequirementResponse)

                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_REQUIREMENT_VALUE, exception.error)
            }
        }

        @Nested
        inner class FReq_1_2_1_4 {

            @Test
            fun `duplicated answer`() {
                val FIRST_ID = "first-requirement-response-id"
                val SECOND_ID = "second-requirement-response-id"
                val THIRD_ID = "third-requirement-response-id"

                val entityNode = entity.toNode()
                val requestNode = json.toNode()

                val lotCriteria = generateLotCriteria(requestNode = requestNode, entityNode = entityNode)
                val lotRequirementId = getLotCriteriaRequirementId(lotCriteria)

                val itemCriteria = generateItemCriteria(requestNode = requestNode, entityNode = entityNode)
                val itemRequirementId = getItemCriteriaRequirementId(itemCriteria)

                val sampleRequirementResponse = getSampleRequirementResponse(requestNode = requestNode)

                val lotRequirementResponse = sampleRequirementResponse.deepCopy()
                lotRequirementResponse.putAttribute("value", 23.56)
                    .putAttribute("id", THIRD_ID)
                    .getObject("requirement")
                    .putAttribute("id", lotRequirementId)

                val itemRequirementResponse = sampleRequirementResponse.deepCopy()
                itemRequirementResponse.putAttribute("value", "SAMPLE_ANSWER")
                    .putAttribute("id", SECOND_ID)
                    .getObject("requirement")
                    .putAttribute("id", itemRequirementId)

                val tenderRequirementResponse = requestNode.getObject("bid")
                    .getArray("requirementResponses").get(1).toObjectNode()
                    .putAttribute("value", 214.04)
                    .putAttribute("id", FIRST_ID)

                requestNode.getObject("bid")
                    .putArray("requirementResponses")
                    .putObject(tenderRequirementResponse)
                    .putObject(lotRequirementResponse)
                    .putObject(itemRequirementResponse)

                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_REQUIREMENT_VALUE, exception.error)
            }
        }

        @Nested
        inner class FReq_1_2_1_5 {

            @Test
            fun `Period endDate greater than current time`() {
                val FIRST_ID = "first-requirement-response-id"

                val requestNode = json.toNode()

                val tenderRequirementResponse = requestNode.getObject("bid")
                    .getArray("requirementResponses").get(1).toObjectNode()
                    .putAttribute("id", FIRST_ID)

                val time = JsonDateTimeSerializer.serialize(LocalDateTime.now().plusMonths(3))
                tenderRequirementResponse.getObject("period")
                    .putAttribute("endDate", time)

                requestNode.getObject("bid")
                    .putArray("requirementResponses")
                    .putObject(tenderRequirementResponse)

                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_PERIOD_VALUE, exception.error)
            }
        }

        @Nested
        inner class FReq_1_2_1_7 {

            @Test
            fun `Period startDate greater than endDate`() {
                val FIRST_ID = "first-requirement-response-id"

                val requestNode = json.toNode()

                val tenderRequirementResponse = requestNode.getObject("bid")
                    .getArray("requirementResponses").get(1).toObjectNode()
                    .putAttribute("id", FIRST_ID)

                val startDate = JsonDateTimeSerializer.serialize(LocalDateTime.now().plusMonths(3))
                val endDate = JsonDateTimeSerializer.serialize(LocalDateTime.now().minusMonths(3))
                tenderRequirementResponse.getObject("period")
                    .putAttribute("startDate", startDate)
                    .putAttribute("endDate", endDate)

                requestNode.getObject("bid")
                    .putArray("requirementResponses")
                    .putObject(tenderRequirementResponse)

                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_PERIOD_VALUE, exception.error)
            }
        }

        @Nested
        inner class FReq_1_2_1_6 {

            @Test
            fun `Period startDate greater than endDate`() {
                val FIRST_ID = "first-requirement-response-id"
                val SECOND_ID = "second-requirement-response-id"
                val THIRD_ID = "third-requirement-response-id"

                val entityNode = entity.toNode()
                val requestNode = json.toNode()

                val lotCriteria = generateLotCriteria(requestNode = requestNode, entityNode = entityNode)
                val lotRequirementId = getLotCriteriaRequirementId(lotCriteria)

                val itemCriteria = generateItemCriteria(requestNode = requestNode, entityNode = entityNode)
                val itemRequirementId = getItemCriteriaRequirementId(itemCriteria)

                val sampleRequirementResponse = getSampleRequirementResponse(requestNode = requestNode)

                val lotRequirementResponse = sampleRequirementResponse.deepCopy()
                lotRequirementResponse.putAttribute("value", 23.56)
                    .putAttribute("id", THIRD_ID)
                    .getObject("requirement")
                    .putAttribute("id", lotRequirementId)

                val startDate = JsonDateTimeSerializer.serialize(LocalDateTime.now().plusMonths(3))
                val endDate = JsonDateTimeSerializer.serialize(LocalDateTime.now().minusMonths(3))
                lotRequirementResponse.getObject("period")
                    .putAttribute("startDate", startDate)
                    .putAttribute("endDate", endDate)

                val itemRequirementResponse = sampleRequirementResponse.deepCopy()
                itemRequirementResponse.putAttribute("value", "SAMPLE_ANSWER")
                    .putAttribute("id", SECOND_ID)
                    .getObject("requirement")
                    .putAttribute("id", itemRequirementId)

                val tenderRequirementResponse = requestNode.getObject("bid")
                    .getArray("requirementResponses").get(1).toObjectNode()
                    .putAttribute("id", FIRST_ID)

                val duplicatedTenderRequirementResponse = tenderRequirementResponse.deepCopy()
                    .putAttribute("id" , FIRST_ID)

                requestNode.getObject("bid")
                    .putArray("requirementResponses")
                    .putObject(tenderRequirementResponse)
                    .putObject(duplicatedTenderRequirementResponse)
                    .putObject(lotRequirementResponse)
                    .putObject(itemRequirementResponse)

                val cnEntity = CnEntity(cpid = CPID, owner = "SOME_OWNER", jsonData = entity)
                whenever(criteriaRepository.findBy(any()))
                    .thenReturn(cnEntity)

                val context = CheckResponsesContext(cpid = "cpid", owner = "owner")
                val cm = commandMessage(CommandType.CHECK_RESPONSES, data = requestNode)

                val exception = assertThrows<ErrorException> { criteriaService.checkResponses(cm, context = context) }
                assertEquals(ErrorType.INVALID_REQUIREMENT_VALUE, exception.error)
            }
        }

        private fun JsonNode.toObjectNode() = this as ObjectNode
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
