package com.procurement.dossier.application.model

import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ParseParamsTest {

    companion object {
        private const val GPA_NAME = "GPA"
    }

    @Nested
    inner class ParsePmd {

        @Test
        fun success() {
            val actual = parsePmd(value = GPA_NAME, allowedValues = ProcurementMethod.allowedElements).get
            val expected = ProcurementMethod.GPA
            assertEquals(expected, actual)
        }

        @Test
        fun unallowedValue_fail() {
            val actual = parsePmd(value = "random string", allowedValues = ProcurementMethod.allowedElements)

            assertTrue(actual.isFail)
        }

        @Test
        fun unallowedEnumValue_fail() {
            val valuesWithoutGpa = ProcurementMethod.allowedElements - ProcurementMethod.GPA
            val actual = parsePmd(value = GPA_NAME, allowedValues = valuesWithoutGpa)
            val expectedDescription = "Attribute value mismatch with one of enum expected values. Expected values: 'MV, OT, RT, SV, DA, NP, FA, OP, TEST_OT, TEST_SV, TEST_RT, TEST_MV, TEST_DA, TEST_NP, TEST_FA, TEST_OP, TEST_GPA', actual value: 'GPA'."

            assertEquals(expectedDescription, actual.error.description)
        }
    }
}