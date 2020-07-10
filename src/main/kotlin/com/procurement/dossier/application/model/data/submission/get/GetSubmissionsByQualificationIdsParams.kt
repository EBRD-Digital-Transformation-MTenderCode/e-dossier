package com.procurement.dossier.application.model.data.submission.get

import com.procurement.dossier.domain.fail.error.DataErrors

class GetSubmissionsByQualificationIdsParams private constructor(
    val cpid: String,
    val ocid: String,
    val qualifications: List<Qualification>
) {
    companion object {
        fun tryCreate() : Result<GetSubmissionsByQualificationIdsParams, DataErrors>{

        }
    }

    data class Qualification(
        val id: String
    )
}