package com.procurement.dossier.domain.model.document

import com.procurement.dossier.domain.util.Result

typealias DocumentId = String

fun String.tryDocumentId(): Result<DocumentId, String> =
    Result.success(this)