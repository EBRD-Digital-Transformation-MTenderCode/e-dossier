package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionCredentials
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.model.submission.SubmissionState
import com.procurement.dossier.domain.util.MaybeFail
import com.procurement.dossier.domain.util.Result

interface SubmissionRepository {
    fun saveSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): MaybeFail<Fail.Incident>

    fun getSubmissionsStates(
        cpid: Cpid,
        ocid: Ocid,
        submissionIds: List<SubmissionId>
    ): Result<List<SubmissionState>, Fail.Incident>

    fun updateSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): Result<Boolean, Fail.Incident>

    fun getSubmissionCredentials(
        cpid: Cpid,
        ocid: Ocid,
        id: SubmissionId
    ): Result<SubmissionCredentials?, Fail.Incident>

    fun findSubmission(cpid: Cpid, ocid: Ocid, id: SubmissionId): Result<Submission?, Fail.Incident>

    fun findBy(cpid: Cpid, ocid: Ocid): Result<List<Submission>, Fail.Incident>

    fun findBy(cpid: Cpid, ocid: Ocid, submissionIds: List<SubmissionId>): Result<List<Submission>, Fail.Incident>

}