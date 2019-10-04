package com.procurement.procurer.infrastructure.model.dto.tender

import com.procurement.procurer.infrastructure.model.dto.ocds.ConversionsRelatesTo

data class Conversion(
    val id: String,
    val relatesTo: ConversionsRelatesTo,
    val relatedItem: String,
    val rationale: String,
    val coefficients: List<Coefficient>
)