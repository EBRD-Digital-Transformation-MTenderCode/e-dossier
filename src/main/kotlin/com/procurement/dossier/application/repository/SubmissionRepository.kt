package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.model.submission.SubmissionId
import com.procurement.dossier.domain.model.submission.SubmissionState
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.ValidationResult

interface SubmissionRepository {
     fun saveSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): ValidationResult<Fail.Incident>
     fun getSubmissionsStates(cpid: Cpid, ocid: Ocid, submissionIds: List<SubmissionId>): Result<List<SubmissionState>, Fail.Incident>
}