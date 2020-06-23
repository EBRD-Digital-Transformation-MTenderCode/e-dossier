package com.procurement.dossier.application.service.params

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseDate
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import java.time.LocalDateTime

class VerifySubmissionPeriodEndParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid,
    val date: LocalDateTime
) {
    companion object {
        fun tryCreate(
            cpid: String,
            ocid: String,
            date: String
        ): Result<VerifySubmissionPeriodEndParams, DataErrors> {

            val parsedCpid = parseCpid(value = cpid)
                .orForwardFail { fail -> return fail }
            val parsedOcid = parseOcid(value = ocid)
                .orForwardFail { fail -> return fail }
            val parsedDate = parseDate(value = date, attributeName = "date")
                .orForwardFail { fail -> return fail }

            return VerifySubmissionPeriodEndParams(cpid = parsedCpid,ocid = parsedOcid,date = parsedDate)
                .asSuccess()
        }
    }
}
