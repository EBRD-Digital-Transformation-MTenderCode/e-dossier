package com.procurement.dossier.domain.model.requirement.response

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result

typealias RequirementResponseId = String

fun String.tryRequirementResponseId(): Result<RequirementResponseId, Fail.Incident.Transforming> =
    Result.success(this)