package com.procurement.dossier.domain.model.evidence

import com.procurement.dossier.domain.util.Result

typealias EvidenceId = String

fun String.tryEvidenceIdId(): Result<EvidenceId, String> =
    Result.success(this)