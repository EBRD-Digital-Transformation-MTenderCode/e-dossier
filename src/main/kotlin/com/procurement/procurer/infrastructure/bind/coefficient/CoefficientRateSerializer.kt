package com.procurement.procurer.infrastructure.bind.coefficient

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.procurement.procurer.application.model.data.CoefficientRate
import java.io.IOException
import java.math.BigDecimal

class CoefficientRateSerializer : JsonSerializer<CoefficientRate>() {
    companion object {
        fun serialize(CoefficientRate: CoefficientRate.AsNumber): BigDecimal = CoefficientRate.value
        fun serialize(CoefficientRate: CoefficientRate.AsInteger): Long = CoefficientRate.value
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        CoefficientRate: CoefficientRate,
        jsonGenerator: JsonGenerator,
        provider: SerializerProvider
    ) =
        when (CoefficientRate) {
            is CoefficientRate.AsNumber -> jsonGenerator.writeNumber(serialize(CoefficientRate))
            is CoefficientRate.AsInteger -> jsonGenerator.writeNumber(serialize(CoefficientRate))
        }
}
