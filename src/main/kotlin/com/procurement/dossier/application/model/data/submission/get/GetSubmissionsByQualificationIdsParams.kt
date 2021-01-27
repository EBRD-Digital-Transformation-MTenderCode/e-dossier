package com.procurement.dossier.application.model.data.submission.get

import com.procurement.dossier.application.model.noDuplicatesRule
import com.procurement.dossier.application.model.notEmptyRule
import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parseQualificationId
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.qualification.QualificationId
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.validate

class GetSubmissionsByQualificationIdsParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid,
    val qualifications: List<Qualification>
) {
    companion object {

        private const val QUALIFICATIONS_ATTRIBUTE_NAME = "qualifications"
        private const val QUALIFICATIONS_ID_ATTRIBUTE_NAME = "qualifications.id"

        fun tryCreate(
            cpid: String,
            ocid: String,
            qualifications: List<Qualification>
        ): Result<GetSubmissionsByQualificationIdsParams, DataErrors> {

            val cpidParsed = parseCpid(cpid).orForwardFail { error -> return error }
            val ocidParsed = parseOcid(ocid).orForwardFail { error -> return error }

            qualifications
                .validate(notEmptyRule(QUALIFICATIONS_ATTRIBUTE_NAME))
                .orForwardFail { fail -> return fail }
                .validate(noDuplicatesRule(QUALIFICATIONS_ID_ATTRIBUTE_NAME) { it.id })
                .orForwardFail { fail -> return fail }

            return GetSubmissionsByQualificationIdsParams(
                cpid = cpidParsed,
                ocid = ocidParsed,
                qualifications = qualifications
            ).asSuccess()
        }
    }

    class Qualification private constructor(
        val id: QualificationId,
        val relatedSubmission: SubmissionId
    ) {
        companion object {

            fun tryCreate(id: String, relatedSubmission: String): Result<Qualification, DataErrors> {
                val parsedId = parseQualificationId(value = id, attributeName = QUALIFICATIONS_ID_ATTRIBUTE_NAME)
                    .orForwardFail { error -> return error }

                val parsedRelatedSubmission = SubmissionId.create(relatedSubmission)

                return Qualification(id = parsedId, relatedSubmission = parsedRelatedSubmission).asSuccess()
            }
        }
    }
}