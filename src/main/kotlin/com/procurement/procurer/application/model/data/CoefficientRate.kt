package com.procurement.procurer.application.model.data

import java.math.BigDecimal

sealed class CoefficientRate {
    data class AsNumber(val value: BigDecimal) : CoefficientRate()
    data class AsInteger(val value: Long) : CoefficientRate()
}