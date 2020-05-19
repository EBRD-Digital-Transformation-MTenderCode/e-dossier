package com.procurement.dossier.application.service

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.data.period.check.CheckPeriodContext
import com.procurement.dossier.application.model.data.period.check.CheckPeriodData
import com.procurement.dossier.application.model.data.period.check.CheckPeriodResult
import com.procurement.dossier.application.model.data.period.save.SavePeriodContext
import com.procurement.dossier.application.model.data.period.save.SavePeriodData
import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodContext
import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodData
import com.procurement.dossier.application.repository.PeriodRepository
import com.procurement.dossier.application.repository.PeriodRulesRepository
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import com.procurement.dossier.infrastructure.model.entity.PeriodEntity
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
        private val CPID = Cpid.tryCreateOrNull("ocds-t1s2t3-MD-1565251033096")!!
        private val OCID = Ocid.tryCreateOrNull("ocds-b3wdp1-MD-1581509539187-EV-1581509653044")!!

        private const val FORMAT_PATTERN = "uuuu-MM-dd'T'HH:mm:ss'Z'"
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN)
            .withResolverStyle(ResolverStyle.STRICT)
        private val DATE = LocalDateTime.parse("2020-02-10T08:49:55Z", FORMATTER)
        private val ENTITY_START_DATE = LocalDateTime.parse("2020-02-12T08:49:55Z", FORMATTER)
        private val ENTITY_END_DATE = ENTITY_START_DATE.plusDays(10)

        private val periodRulesRepository: PeriodRulesRepository = mock()
        private val periodRepository: PeriodRepository = mock()
        private val periodService: PeriodService = PeriodService(periodRulesRepository, periodRepository)

    }

    @Nested
    inner class Validate {

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

            periodService.validatePeriod(data = data, context = context)
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

        private fun stubContext() = ValidatePeriodContext(pmd = PMD, country = COUNTRY)
    }

    @Nested
    inner class Check {

        @Test
        fun requestEndDateIsAfterStoredEndDate_success() {
            val periodEntity = stubPeriodEntity()
            whenever(periodRepository.findBy(cpid = CPID, ocid = OCID))
                .thenReturn(periodEntity)

            val startDate = DATE
            val endDate = periodEntity.endDate.plusDays(1)
            val data = createCheckPeriodData(startDate = startDate, endDate = endDate)

            val actual = periodService.checkPeriod(data = data, context = stubContext())

            val expected = CheckPeriodResult(
                isPreQualificationPeriodChanged = true,
                preQualification = CheckPeriodResult.PreQualification(
                    period = CheckPeriodResult.PreQualification.Period(
                        startDate = periodEntity.startDate, endDate = endDate
                    )
                )
            )

            assertEquals(expected, actual)
        }

        @Test
        fun requestEndDateEqualsStoredEndDate_success() {
            val periodEntity = stubPeriodEntity()
            whenever(periodRepository.findBy(cpid = CPID, ocid = OCID))
                .thenReturn(periodEntity)

            val startDate = DATE
            val endDate = periodEntity.endDate
            val data = createCheckPeriodData(startDate = startDate, endDate = endDate)

            val actual = periodService.checkPeriod(data = data, context = stubContext())

            val expected = CheckPeriodResult(
                isPreQualificationPeriodChanged = false,
                preQualification = CheckPeriodResult.PreQualification(
                    period = CheckPeriodResult.PreQualification.Period(
                        startDate = periodEntity.startDate,
                        endDate = periodEntity.endDate
                    )
                )
            )

            assertEquals(expected, actual)
        }

        @Test
        fun requestEndDatePrecedesStartDate_exception() {
            val periodEntity = stubPeriodEntity()
            whenever(periodRepository.findBy(cpid = CPID, ocid = OCID))
                .thenReturn(periodEntity)

            val startDate = DATE
            val endDate = startDate.minusDays(2)
            val data = createCheckPeriodData(startDate = startDate, endDate = endDate)

            val exception = assertThrows<ErrorException> {
                periodService.checkPeriod(data = data, context = stubContext())
            }
            val actual = exception.code
            val expected = ErrorType.INVALID_PERIOD_DATES.code

            assertEquals(expected, actual)
        }

        @Test
        fun requestEndDatePrecedesStoredEndDate_exception() {
            val periodEntity = stubPeriodEntity()
            whenever(periodRepository.findBy(cpid = CPID, ocid = OCID))
                .thenReturn(periodEntity)

            val startDate = DATE
            val endDate = periodEntity.endDate.minusSeconds(1)
            val data = createCheckPeriodData(startDate = startDate, endDate = endDate)

            val exception = assertThrows<ErrorException> {
                periodService.checkPeriod(data = data, context = stubContext())
            }
            val actual = exception.code
            val expected = ErrorType.INVALID_PERIOD_END_DATE.code

            assertEquals(expected, actual)
        }

        private fun createCheckPeriodData(startDate: LocalDateTime, endDate: LocalDateTime) =
            CheckPeriodData(period = CheckPeriodData.Period(startDate = startDate, endDate = endDate))


        private fun stubPeriodEntity() =
            PeriodEntity(
                startDate = ENTITY_START_DATE,
                endDate = ENTITY_END_DATE,
                ocid = OCID,
                cpid = CPID
            )

        private fun stubContext() = CheckPeriodContext(cpid = CPID, ocid = OCID)
    }

    @Nested
    inner class SavePeriod {
        @Test
        fun repositoryMethod_isCalled() {
            val startDate = DATE
            val endDate = startDate.plusDays(1)
            val data = createSavePeriodData(startDate = startDate, endDate = endDate)
            val context = SavePeriodContext(cpid = CPID, ocid = OCID)

            periodService.savePeriod(data = data, context = context)

            verify(periodRepository).saveOrUpdatePeriod(
                period = PeriodEntity(
                    cpid = context.cpid,
                    ocid = context.ocid,
                    endDate = data.period.endDate,
                    startDate = data.period.startDate
                )
            )
        }

        @Test
        fun success() {
            val startDate = DATE
            val endDate = startDate.plusDays(2)
            val data = createSavePeriodData(startDate = startDate, endDate = endDate)
            val context = SavePeriodContext(cpid = CPID, ocid = OCID)

            periodService.savePeriod(data = data, context = context)
        }


        private fun createSavePeriodData(startDate: LocalDateTime, endDate: LocalDateTime) =
            SavePeriodData(
                period = SavePeriodData.Period(
                    startDate = startDate, endDate = endDate
                )
            )
    }
}
