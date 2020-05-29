package com.procurement.dossier.domain.model

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.extension.tryUUID
import java.util.*

typealias Token = UUID

fun String.tryToken(): Result<Token, Fail.Incident.Transform.Parsing> =
    this.tryUUID()