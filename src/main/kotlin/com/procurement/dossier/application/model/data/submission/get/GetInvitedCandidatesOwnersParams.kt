package com.procurement.dossier.application.model.data.submission.get

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess

class GetInvitedCandidatesOwnersParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid
) {
    companion object {
        fun tryCreate(cpid: String, ocid: String): Result<GetInvitedCandidatesOwnersParams, DataErrors> {
            val cpidParsed = parseCpid(cpid).orForwardFail { return it }
            val ocidParsed = parseOcid(ocid).orForwardFail { return it }

            return GetInvitedCandidatesOwnersParams(cpid = cpidParsed, ocid = ocidParsed).asSuccess()
        }
    }
}