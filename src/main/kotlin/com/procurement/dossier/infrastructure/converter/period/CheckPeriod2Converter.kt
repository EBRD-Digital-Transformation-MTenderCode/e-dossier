package com.procurement.dossier.infrastructure.converter.period

import com.procurement.dossier.application.model.data.period.check.params.CheckPeriod2Params
import com.procurement.dossier.infrastructure.model.dto.request.period.CheckPeriod2Request

fun CheckPeriod2Request.convert() = CheckPeriod2Params.tryCreate(cpid = cpid, ocid = ocid, date = date)
