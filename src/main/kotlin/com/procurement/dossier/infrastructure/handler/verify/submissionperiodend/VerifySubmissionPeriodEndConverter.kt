package com.procurement.dossier.infrastructure.handler.verify.submissionperiodend

import com.procurement.dossier.application.service.params.VerifySubmissionPeriodEndParams
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.Result

fun VerifySubmissionPeriodEndRequest.convert(): Result<VerifySubmissionPeriodEndParams, DataErrors> =
    VerifySubmissionPeriodEndParams.tryCreate(cpid = this.cpid, date = this.date, ocid = this.ocid)
