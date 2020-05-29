package com.procurement.dossier.infrastructure.converter.period

import com.procurement.dossier.application.model.data.period.save.SavePeriodData
import com.procurement.dossier.infrastructure.model.dto.request.period.SavePeriodRequest

fun SavePeriodRequest.convert() =
    SavePeriodData(
        period = period.let { period ->
            SavePeriodData.Period(
                startDate = period.startDate,
                endDate = period.endDate
            )
        }
    )