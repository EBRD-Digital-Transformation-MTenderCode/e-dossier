package com.procurement.dossier.application.service

import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodContext
import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodData
import com.procurement.dossier.application.repository.PeriodRulesRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PeriodService(private val periodRulesRepository: PeriodRulesRepository) {

    fun validatePeriod(data: ValidatePeriodData, context: ValidatePeriodContext) {
        val period = data.period
        checkPeriodDates(startDate = period.startDate, endDate = period.endDate)
        checkPeriodDuration(period, context)
    }

    private fun checkPeriodDates(startDate: LocalDateTime, endDate: LocalDateTime) {
        if (!startDate.isBefore(endDate))
            throw ErrorException(ErrorType.INVALID_PERIOD_DATES)
    }

    private fun checkPeriodDuration(period: ValidatePeriodData.Period, context: ValidatePeriodContext) {
        val expectedDuration = periodRulesRepository.findDurationBy(country = context.country, pmd = context.pmd)
            ?: throw ErrorException(
                error = ErrorType.PERIOD_RULE_NOT_FOUND,
                message = "No period duration rule found for country '${context.country}' and pmd '${context.pmd}'."
            )

        val actualDuration = ChronoUnit.SECONDS.between(period.startDate, period.endDate)

        if (actualDuration < expectedDuration)
            throw ErrorException(ErrorType.INVALID_PERIOD_DURATION)
    }
}