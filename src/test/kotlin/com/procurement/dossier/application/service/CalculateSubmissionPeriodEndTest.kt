package com.procurement.dossier.application.service

import com.nhaarman.mockito_kotlin.mock
import com.procurement.dossier.application.repository.PeriodRepository
import com.procurement.dossier.application.repository.PeriodRulesRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CalculateSubmissionPeriodEndTest {

    private val periodRulesRepository: PeriodRulesRepository = mock()
    private val periodRepository: PeriodRepository = mock()
    private val periodService: PeriodService = PeriodService(periodRulesRepository, periodRepository)

    @Test
    fun verifyThatSubmissionPeriodIsExpiredWithoutError() {

        val rqDate = createLTD(1, 2)
        val periodEndDate = createLTD(1, 1)

        val isExpired = periodService.calculateSubmissionPeriodEnd(periodEndDate = periodEndDate, rqDate = rqDate)

        assertTrue(isExpired)
    }

    @Test
    fun verifyThatSubmissionPeriodIsExpiredWithError() {

        val rqDate = createLTD(0, 2)
        val periodEndDate = createLTD(1, 1)

        val isExpired = periodService.calculateSubmissionPeriodEnd(periodEndDate = periodEndDate, rqDate = rqDate)

        assertTrue(isExpired)
    }

    @Test
    fun verifyThatSubmissionPeriodIsNotExpired() {

        val rqDate = createLTD(0, 0)
        val periodEndDate = createLTD(1, 1)

        val isExpired = periodService.calculateSubmissionPeriodEnd(periodEndDate = periodEndDate, rqDate = rqDate)

        assertFalse(isExpired)
    }

    @Test
    fun verifyThatSubmissionPeriodIsNotExpired_EqualDates() {

        val rqDate = createLTD(1, 1)
        val periodEndDate = createLTD(1, 1)

        val isExpired = periodService.calculateSubmissionPeriodEnd(periodEndDate = periodEndDate, rqDate = rqDate)

        assertTrue(isExpired)
    }

    @Test
    fun verifyThatSubmissionPeriodIsNotExpired_EqualDatesWithError() {

        val rqDate = createLTD(0, 1)
        val periodEndDate = createLTD(1, 1)

        val isExpired = periodService.calculateSubmissionPeriodEnd(periodEndDate = periodEndDate, rqDate = rqDate)

        assertFalse(isExpired)
    }

    private fun createLTD(seconds: Int, nanoSeconds: Int) =
        LocalDateTime.of(2000, 5, 1, 1, 1, seconds, nanoSeconds)
}