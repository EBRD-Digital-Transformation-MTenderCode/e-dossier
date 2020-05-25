package com.procurement.dossier.application.model.data.submission.state.get

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parseSubmissionId
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.mapResult

class GetSubmissionStateByIdsParams private constructor(
    val submissionIds: List<SubmissionId>,
    val cpid: Cpid,
    val ocid: Ocid
) {
    companion object {
        private const val SUBMISSION_IDS_ATTRIBUTE_NAME = "submissionIds"
        fun tryCreate(
            submissionIds: List<String>,
            cpid: String,
            ocid: String
        ): Result<GetSubmissionStateByIdsParams, DataErrors> {
            if (submissionIds.isEmpty())
                return Result.failure(DataErrors.Validation.EmptyArray(name = SUBMISSION_IDS_ATTRIBUTE_NAME))

            val cpidParsed = parseCpid(cpid).orForwardFail { error -> return error }
            val ocidParsed = parseOcid(ocid).orForwardFail { error -> return error }
            val submissionIdsParsed = submissionIds.mapResult { parseSubmissionId(it, SUBMISSION_IDS_ATTRIBUTE_NAME) }
                .doReturn { error -> return Result.failure(error = error) }

            return GetSubmissionStateByIdsParams(
                cpid = cpidParsed,
                ocid = ocidParsed,
                submissionIds = submissionIdsParsed
            ).asSuccess()
        }
    }
}