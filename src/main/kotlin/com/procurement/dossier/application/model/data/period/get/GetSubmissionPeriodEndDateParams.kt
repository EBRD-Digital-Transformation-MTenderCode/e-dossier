package com.procurement.dossier.application.model.data.period.get

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess

class GetSubmissionPeriodEndDateParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid
) {
    companion object {
        fun tryCreate(
            cpid: String, ocid: String
        ): Result<GetSubmissionPeriodEndDateParams, DataErrors> {
            val cpidParsed = parseCpid(cpid).doReturn { error -> return Result.failure(error = error) }
            val ocidParsed = parseOcid(ocid).doReturn { error -> return Result.failure(error = error) }

            return GetSubmissionPeriodEndDateParams(
                cpid = cpidParsed,
                ocid = ocidParsed
            ).asSuccess()
        }
    }
}