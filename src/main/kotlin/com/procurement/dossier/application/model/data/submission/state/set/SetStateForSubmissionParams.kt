package com.procurement.dossier.application.model.data.submission.state.set

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parseSubmissionStatus
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess

class SetStateForSubmissionParams private constructor(
    val cpid: Cpid, val ocid: Ocid, val submission: Submission
) {
    companion object {
        fun tryCreate(
            cpid: String, ocid: String, submission: Submission
        ): Result<SetStateForSubmissionParams, DataErrors> {
            val cpidParsed = parseCpid(cpid).orForwardFail { error -> return error }
            val ocidParsed = parseOcid(ocid).orForwardFail { error -> return error }

            return SetStateForSubmissionParams(
                cpid = cpidParsed, ocid = ocidParsed, submission = submission
            ).asSuccess()
        }
    }

    class Submission private constructor(
        val id: SubmissionId, val status: SubmissionStatus
    ) {
        companion object {
            private const val SUBMISSION_ID_ATTRIBUTE_NAME = "submission.id"
            private const val SUBMISSION_STATUS_ATTRIBUTE_NAME = "submission.status"
            private val allowedStatuses = SubmissionStatus.allowedElements
                .filter { value ->
                    when (value) {
                        SubmissionStatus.PENDING,
                        SubmissionStatus.DISQUALIFIED,
                        SubmissionStatus.VALID,
                        SubmissionStatus.WITHDRAWN -> true
                    }
                }

            fun tryCreate(id: String, status: String): Result<Submission, DataErrors> {
                val idParsed = SubmissionId.create(id)

                val statusParsed = parseSubmissionStatus(
                    value = status,
                    attributeName = SUBMISSION_STATUS_ATTRIBUTE_NAME,
                    allowedEnums = allowedStatuses
                ).orForwardFail { fail -> return fail }

                return Submission(id = idParsed, status = statusParsed).asSuccess()
            }
        }
    }
}