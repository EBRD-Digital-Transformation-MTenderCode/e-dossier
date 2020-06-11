package com.procurement.dossier.domain.model.submission

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.extension.tryUUID
import java.util.*

typealias SubmissionId = UUID

fun String.trySubmissionId(): Result<SubmissionId, Fail.Incident.Transform.Parsing> =
    this.tryUUID()