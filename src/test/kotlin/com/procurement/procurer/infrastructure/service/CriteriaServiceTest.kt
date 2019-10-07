package com.procurement.procurer.infrastructure.service

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.infrastructure.generator.CommandMessageGenerator
import com.procurement.procurer.infrastructure.generator.ContextGenerator
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandType
import com.procurement.procurer.infrastructure.model.dto.cn.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender.Criteria.RequirementGroup
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender.Item
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.model.dto.ocds.Operation
import com.procurement.procurer.infrastructure.model.dto.ocds.ProcurementMethod
import com.procurement.procurer.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.procurer.infrastructure.model.dto.data.Requirement
import com.procurement.procurer.json.getObject
import com.procurement.procurer.json.loadJson
import com.procurement.procurer.json.testingBindingAndMapping
import com.procurement.procurer.json.toNode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CriteriaServiceTest {

    private val CHECK_CRITERIA_REQUEST = "json/service/criteria/check/request/request_check_criteria_full.json"

    private val criteriaService: CriteriaService = CriteriaService()
    private val parseContext = JsonPath.using(Configuration.defaultConfiguration())
    private val json = loadJson(CHECK_CRITERIA_REQUEST)

    @BeforeEach
    fun clear() {
        testingBindingAndMapping<CheckCriteriaRequest>(CHECK_CRITERIA_REQUEST)
    }

    @Nested
    inner class CheckCriteria {

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
                requestNode.getObject("tender")
                    .put("awardCriteria", AwardCriteria.fromString(awardCriteria).value)
                    .remove("awardCriteriaDetails")

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

                requestNode.getObject("tender")
                    .put("awardCriteria", AwardCriteria.fromString(awardCriteria).value)
                    .put("awardCriteriaDetails", AwardCriteriaDetails.fromString(awardCriteriaDetails).value)

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

                val exception = assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
                assertTrue(exception.cause is JsonMappingException)
            }

            @Test
            fun `Expected value without minValue || maxValue`() {

                val document = parseContext.parse(json)
                document.put("$.tender.criteria[1].requirementGroups[0].requirements[1]", "expectedValue", 2.0)
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue")
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue")

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
            }

            @Test
            fun `Expected value == number || DataType == number`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "number")
                document.put("$.tender.criteria[1].requirementGroups[0].requirements[1]", "expectedValue", 25.12)
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue")
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue")

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                assertDoesNotThrow { criteriaService.checkCriteria(cm) }
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

                val exception = assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
                assertTrue(exception.cause is JsonMappingException)
            }

            @Test
            fun `Expected value == number || DataType == integer`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "number")
                document.put("$.tender.criteria[1].requirementGroups[0].requirements[1]", "expectedValue", 1)
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue")
                document.delete("$.tender.criteria[1].requirementGroups[0].requirements[1].maxValue")

                val requestNode = document.jsonString().toNode()
                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
                assertTrue(exception.cause is JsonMappingException)
            }
        }

        @Nested
        inner class FReq_1_1_1_5 {

            @Test
            fun `MinValue less than MaxValue`() {

                val document = parseContext.parse(json)
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].dataType", "number")
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue", 123.0)
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
                document.set("$.tender.criteria[1].requirementGroups[0].requirements[1].minValue", 234.14)
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

                val exception = assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
                assertTrue(exception.cause is JsonMappingException)
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
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
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
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
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

            private val MAX = 2
            private val MIN = 0.01

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
                document.set("$.tender.conversions[0].coefficients[0].coefficient", 2.toBigDecimal())

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
                document.set("$.tender.conversions[0].coefficients[0].coefficient", 1.8.toBigDecimal())

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
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", UNIQUE_ID)
                document.set("$.tender.conversions[1].relatedItem", UNIQUE_ID)
                document.set("$.tender.conversions[1].coefficients[0].value", 0.5.toBigDecimal())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CONVERSION, exception.error)
            }

            @Test
            fun `Conversion coefficient == number, Requirement value == number`() {
                val document = parseContext.parse(json)
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].id", UNIQUE_ID)
                document.set(
                    "$.tender.criteria[2].requirementGroups[0].requirements[0].dataType",
                    RequirementDataType.NUMBER
                )
                document.set("$.tender.criteria[2].requirementGroups[0].requirements[0].expectedValue", 123.54)
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

                val exception = assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
                assertTrue(exception.cause is JsonMappingException)
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

                val exception = assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
                assertTrue(exception.cause is JsonMappingException)
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

                val exception = assertThrows<IllegalArgumentException> { criteriaService.checkCriteria(cm) }
                assertTrue(exception.cause is JsonMappingException)
            }
        }

        @Nested
        inner class FReq_1_1_1_16 {

            @Test
            fun `empty items`() {
                val document = parseContext.parse(json)
                document.put("$.tender", "items", emptyList<Item>())

                val requestNode = document.jsonString().toNode()

                val cm = commandMessage(
                    CommandType.CHECK_CRITERIA,
                    data = requestNode
                )

                val exception = assertThrows<ErrorException> { criteriaService.checkCriteria(cm) }
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
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
                assertEquals(ErrorType.INVALID_CRITERIA, exception.error)
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
