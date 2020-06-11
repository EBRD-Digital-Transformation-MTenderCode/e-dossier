package com.procurement.dossier.domain.model.requirement
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result

typealias RequirementId = String

fun String.tryRequirementId(): Result<RequirementId, Fail.Incident.Transform.Parsing> =
    Result.success(this)