package com.procurement.dossier.application.model.data.submission.state.get

import com.procurement.dossier.application.model.noDuplicatesRule
import com.procurement.dossier.application.model.notEmptyRule
import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.validate

class GetSubmissionStateByIdsParams private constructor(
    val submissionIds: List<SubmissionId>,
    val cpid: Cpid,
    val ocid: Ocid
) {
    companion object {
        private const val SUBMISSION_IDS_ATTRIBUTE_NAME = "submissionIds"
        fun tryCreate(
            submissionIds: List<String>, cpid: String, ocid: String
        ): Result<GetSubmissionStateByIdsParams, DataErrors> {
            submissionIds
                .validate(notEmptyRule(SUBMISSION_IDS_ATTRIBUTE_NAME))
                .orForwardFail { fail -> return fail }
                .validate(noDuplicatesRule(SUBMISSION_IDS_ATTRIBUTE_NAME){it})
                .orForwardFail { fail -> return fail }

            val cpidParsed = parseCpid(cpid).orForwardFail { error -> return error }
            val ocidParsed = parseOcid(ocid).orForwardFail { error -> return error }
            val submissionIdsParsed = submissionIds.map { SubmissionId.create(it) }

            return GetSubmissionStateByIdsParams(
                cpid = cpidParsed,
                ocid = ocidParsed,
                submissionIds = submissionIdsParsed
            ).asSuccess()
        }
    }
}