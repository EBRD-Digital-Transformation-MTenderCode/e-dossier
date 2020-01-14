package com.procurement.procurer.application.service.context

import com.procurement.procurer.infrastructure.model.dto.ocds.ProcurementMethod

data class EvPanelsContext(
    val cpid: String,
    val country: String,
    val pmd: ProcurementMethod,
    val language: String
)