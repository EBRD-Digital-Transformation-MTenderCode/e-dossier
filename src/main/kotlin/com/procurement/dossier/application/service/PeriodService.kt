package com.procurement.dossier.application.service

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
import com.procurement.dossier.infrastructure.model.entity.PeriodEntity
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PeriodService(
    private val periodRulesRepository: PeriodRulesRepository,
    private val periodRepository: PeriodRepository
) {
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

    fun checkPeriod(data: CheckPeriodData, context: CheckPeriodContext): CheckPeriodResult{
        val requestPeriod = data.period
        checkPeriodDates(startDate = requestPeriod.startDate, endDate = requestPeriod.endDate)

        val storedPeriod = periodRepository.findBy(cpid = context.cpid, ocid = context.ocid)!!
        requestPeriod.validateGreaterOrEq(storedPeriod = storedPeriod)

        return CheckPeriodResult(
            isPreQualificationPeriodChanged = isPreQualificationPeriodChanged(requestPeriod, storedPeriod),
            preQualification = CheckPeriodResult.PreQualification(
                period = CheckPeriodResult.PreQualification.Period(
                    startDate = storedPeriod.startDate,
                    endDate = requestPeriod.endDate
                )
            )
        )
    }

    private fun CheckPeriodData.Period.validateGreaterOrEq(storedPeriod: PeriodEntity) {
        if (endDate.isBefore(storedPeriod.endDate))
            throw ErrorException(ErrorType.INVALID_PERIOD_END_DATE)
    }

    private fun isPreQualificationPeriodChanged(
        requestPeriod: CheckPeriodData.Period, storedPeriod: PeriodEntity
    ) = !requestPeriod.endDate.isEqual(storedPeriod.endDate)

    fun savePeriod(data: SavePeriodData, context: SavePeriodContext) =
        periodRepository.saveOrUpdatePeriod(
            PeriodEntity(
                cpid = context.cpid,
                ocid = context.ocid,
                endDate = data.period.endDate,
                startDate = data.period.startDate
            )
        )
}