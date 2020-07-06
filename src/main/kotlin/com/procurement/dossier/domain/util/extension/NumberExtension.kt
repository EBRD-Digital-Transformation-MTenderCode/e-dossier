package com.procurement.dossier.domain.util.extension

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess

fun String.tryToLong(): Result<Long, Fail.Incident.Transform.Parsing> = try {
    this.toLong().asSuccess()
} catch (exception: NumberFormatException) {
    Fail.Incident.Transform.Parsing(className = Long::class.java.canonicalName, exception = exception).asFailure()
}