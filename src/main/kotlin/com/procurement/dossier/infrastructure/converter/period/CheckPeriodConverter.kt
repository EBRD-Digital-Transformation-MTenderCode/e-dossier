package com.procurement.dossier.infrastructure.converter.period

import com.procurement.dossier.application.model.data.period.check.CheckPeriodData
import com.procurement.dossier.infrastructure.model.dto.request.period.CheckPeriodRequest

fun CheckPeriodRequest.convert() =
    CheckPeriodData(
        period = period.let { period ->
            CheckPeriodData.Period(
                startDate = period.startDate,
                endDate = period.endDate
            )
        }
    )