package com.procurement.dossier.application.service

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodContext
import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodData
import com.procurement.dossier.application.repository.PeriodRulesRepository
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.concurrent.TimeUnit

internal class PeriodServiceTest {
    companion object {
        private const val COUNTRY = "MD"
        private val PMD = ProcurementMethod.GPA
        private val ALLOWED_TERM = TimeUnit.DAYS.toSeconds(10)

        private const val FORMAT_PATTERN = "uuuu-MM-dd'T'HH:mm:ss'Z'"
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN)
            .withResolverStyle(ResolverStyle.STRICT)
        private val DATE = LocalDateTime.parse("2020-02-10T08:49:55Z", FORMATTER)
    }

    @Nested
    inner class Validate {

        private val periodRulesRepository: PeriodRulesRepository = mock()
        private val periodService: PeriodService = PeriodService(periodRulesRepository)

        @Test
        fun periodDurationEqualsAllowedTerm_success() {
            whenever(periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY))
                .thenReturn(ALLOWED_TERM)

            val endDate = DATE.plusDays(10)
            val startDate = DATE
            val data = createValidatePeriodData(startDate = startDate, endDate = endDate)
            val context = stubContext()

            periodService.validatePeriod(data = data, context = context)
        }

        @Test
        fun periodDurationGreaterThanAllowedTerm_success() {
            whenever(periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY))
                .thenReturn(ALLOWED_TERM)

            val endDate = DATE.plusDays(10).plusSeconds(1)
            val startDate = DATE
            val data = createValidatePeriodData(startDate = startDate, endDate = endDate)
            val context = stubContext()

            val actual = periodService.validatePeriod(data = data, context = context)
        }

        @Test
        fun startAndEndDateEqual_exception() {
            whenever(periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY))
                .thenReturn(ALLOWED_TERM)

            val data = createValidatePeriodData(startDate = DATE, endDate = DATE)
            val context = stubContext()

            val exception = assertThrows<ErrorException> {
                periodService.validatePeriod(data = data, context = context)
            }
            val expectedErrorCode = ErrorType.INVALID_PERIOD_DATES.code

            assertEquals(expectedErrorCode, exception.code)
        }

        @Test
        fun endDatePrecedesStartDate_exception() {
            whenever(periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY))
                .thenReturn(ALLOWED_TERM)

            val startDate = DATE.plusDays(10).plusSeconds(1)
            val endDate = DATE

            val data = createValidatePeriodData(startDate = startDate, endDate = endDate)
            val context = stubContext()

            val exception = assertThrows<ErrorException> {
                periodService.validatePeriod(data = data, context = context)
            }
            val expectedErrorCode = ErrorType.INVALID_PERIOD_DATES.code

            assertEquals(expectedErrorCode, exception.code)
        }

        @Test
        fun periodDurationLessThanTenDaysByOneSecond_exception() {
            whenever(periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY))
                .thenReturn(ALLOWED_TERM)

            val endDate = DATE.plusDays(10).minusSeconds(1)
            val startDate = DATE
            val data = createValidatePeriodData(startDate = startDate, endDate = endDate)
            val context = stubContext()

            val exception = assertThrows<ErrorException> {
                periodService.validatePeriod(data = data, context = context)
            }
            val expectedErrorCode = ErrorType.INVALID_PERIOD_DURATION.code

            assertEquals(expectedErrorCode, exception.code)
        }

        @Test
        fun periodDurationRuleNotFound_exception() {
            whenever(periodRulesRepository.findDurationBy(pmd = PMD, country = COUNTRY))
                .thenReturn(null)

            val endDate = DATE.plusDays(10)
            val startDate = DATE
            val data = createValidatePeriodData(startDate = startDate, endDate = endDate)
            val context = stubContext()

            val exception = assertThrows<ErrorException> {
                periodService.validatePeriod(data = data, context = context)
            }
            val expectedErrorCode = ErrorType.PERIOD_RULE_NOT_FOUND.code

            assertEquals(expectedErrorCode, exception.code)
        }

        private fun createValidatePeriodData(startDate: LocalDateTime, endDate: LocalDateTime) = ValidatePeriodData(
            ValidatePeriodData.Period(startDate = startDate, endDate = endDate)
        )
    }

    private fun stubContext() = ValidatePeriodContext(pmd = PMD, country = COUNTRY)
}
