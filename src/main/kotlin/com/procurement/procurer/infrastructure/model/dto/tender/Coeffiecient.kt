package com.procurement.procurer.infrastructure.model.dto.tender

import java.math.BigDecimal

data class Coefficient(
    val id: String,
    val value: CoefficientValue,
    val coefficient: BigDecimal
)