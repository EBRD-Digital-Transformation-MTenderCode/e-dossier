package com.procurement.dossier.infrastructure.converter.period

import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodData
import com.procurement.dossier.infrastructure.model.dto.request.period.ValidatePeriodRequest

fun ValidatePeriodRequest.convert() =
    ValidatePeriodData(
        period = period.let { period ->
            ValidatePeriodData.Period(
                startDate = period.startDate,
                endDate = period.endDate
            )
        }
    )