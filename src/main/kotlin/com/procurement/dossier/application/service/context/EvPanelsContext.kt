package com.procurement.dossier.application.service.context

import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod

data class EvPanelsContext(
    val cpid: String,
    val country: String,
    val pmd: ProcurementMethod,
    val language: String
)