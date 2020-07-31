package com.procurement.dossier.application.model.data.submission.finalize

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parseQualificationId
import com.procurement.dossier.application.model.parseQualificationStatus
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.qualification.QualificationId
import com.procurement.dossier.domain.model.qualification.QualificationStatus
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.asSuccess

class FinalizeSubmissionsParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid,
    val qualifications: List<Qualification>
) {
    companion object {

        fun tryCreate(
            cpid: String,
            ocid: String,
            qualifications: List<Qualification>
        ): Result<FinalizeSubmissionsParams, DataErrors.Validation> {
            val cpidParsed = parseCpid(cpid)
                .doReturn { error -> return failure(error = error) }

            val ocidParsed = parseOcid(ocid)
                .doReturn { error -> return failure(error = error) }

            return FinalizeSubmissionsParams(
                cpid = cpidParsed,
                ocid = ocidParsed,
                qualifications = qualifications
            ).asSuccess()
        }
    }

    class Qualification private constructor(
        val id: QualificationId,
        val status: QualificationStatus,
        val relatedSubmission: SubmissionId
    ) {
        companion object {
            private const val ID_ATTRIBUTE_NAME = "qualification.id"
            private const val STATUS_ATTRIBUTE_NAME = "qualification.status"

            private val allowedStatuses = QualificationStatus.allowedElements
                .filter { value ->
                    when (value) {
                        QualificationStatus.ACTIVE,
                        QualificationStatus.UNSUCCESSFUL -> true
                    }
                }
                .toSet()

            fun tryCreate(
                id: String,
                status: String,
                relatedSubmission: String
            ): Result<Qualification, DataErrors.Validation> {

                val idParsed = parseQualificationId(id, ID_ATTRIBUTE_NAME)
                    .doReturn { error -> return failure(error = error) }

                val parsedRelatedSubmission = SubmissionId.create(relatedSubmission)

                val parsedStatus = parseQualificationStatus(status, allowedStatuses, STATUS_ATTRIBUTE_NAME)
                    .doReturn { error -> return failure(error = error) }

                return Qualification(id = idParsed, status = parsedStatus, relatedSubmission = parsedRelatedSubmission)
                    .asSuccess()
            }
        }
    }
}