package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.model.submission.Submission

interface SubmissionRepository {
     fun saveSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): ValidationResult<Fail.Incident>
}