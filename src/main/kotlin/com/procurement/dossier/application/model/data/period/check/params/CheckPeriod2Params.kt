package com.procurement.dossier.application.model.data.period.check.params


import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseDate
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import java.time.LocalDateTime

class CheckPeriod2Params private constructor(
    val cpid: Cpid, val ocid: Ocid, val date: LocalDateTime
) {
    companion object {
        private const val DATE_ATTRIBUTE_NAME = "date"

        fun tryCreate(cpid: String, ocid: String, date: String): Result<CheckPeriod2Params, DataErrors> {
            val cpidParsed = parseCpid(cpid)
                .orForwardFail { error -> return error }
            val ocidParsed = parseOcid(ocid)
                .orForwardFail { error -> return error }
            val dateParsed = parseDate(value = date, attributeName = DATE_ATTRIBUTE_NAME)
                .orForwardFail { error -> return error }

            return CheckPeriod2Params(
                cpid = cpidParsed, ocid = ocidParsed, date = dateParsed
            ).asSuccess()
        }
    }
}