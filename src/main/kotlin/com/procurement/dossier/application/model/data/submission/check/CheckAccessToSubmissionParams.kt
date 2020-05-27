package com.procurement.dossier.application.model.data.submission.check

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parseOwner
import com.procurement.dossier.application.model.parseSubmissionId
import com.procurement.dossier.application.model.parseToken
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.Owner
import com.procurement.dossier.domain.model.Token
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess

class CheckAccessToSubmissionParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid,
    val token: Token,
    val owner: Owner,
    val submissionId: SubmissionId
) {
    companion object {

        private const val SUBMISSION_ID_ATTRIBUTE_NAME = "submissionId"

        fun tryCreate(
            cpid: String, ocid: String, token: String, owner: String, submissionId: String
        ): Result<CheckAccessToSubmissionParams, DataErrors> {
            val cpidParsed = parseCpid(cpid).orForwardFail { error -> return error }
            val ocidParsed = parseOcid(ocid).orForwardFail { error -> return error }
            val submissionIdsParsed = parseSubmissionId(submissionId, SUBMISSION_ID_ATTRIBUTE_NAME)
                .orForwardFail { error -> return error }
            val tokenParsed = parseToken(token).orForwardFail { error -> return error }
            val ownerParsed = parseOwner(owner).orForwardFail { error -> return error }

            return CheckAccessToSubmissionParams(
                cpid = cpidParsed,
                ocid = ocidParsed,
                token = tokenParsed,
                owner = ownerParsed,
                submissionId = submissionIdsParsed
            ).asSuccess()
        }
    }
}