package com.procurement.dossier.application.repository

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.submission.Submission
import com.procurement.dossier.domain.util.MaybeFail

interface SubmissionRepository {
     fun saveSubmission(cpid: Cpid, ocid: Ocid, submission: Submission): MaybeFail<Fail.Incident>
}