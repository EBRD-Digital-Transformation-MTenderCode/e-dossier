package com.procurement.dossier.domain.util.extension

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import java.util.*

fun String.tryUUID(): Result<UUID, Fail.Incident.Transform.Parsing> =
    try {
        UUID.fromString(this).asSuccess()
    } catch (ex: Exception) {
        Fail.Incident.Transform.Parsing(UUID::class.java.canonicalName, ex).asFailure()
    }