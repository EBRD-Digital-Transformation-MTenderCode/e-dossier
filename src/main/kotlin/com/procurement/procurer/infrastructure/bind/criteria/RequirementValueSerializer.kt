package com.procurement.procurer.infrastructure.bind.criteria

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.procurement.procurer.application.model.data.RequirementRsValue
import java.io.IOException

class RequirementValueSerializer : JsonSerializer<RequirementRsValue>() {
    companion object {
        fun serialize(requirementValue: RequirementRsValue.AsString): String = requirementValue.value
        fun serialize(requirementValue: RequirementRsValue.AsBoolean): Boolean = requirementValue.value
        fun serialize(requirementValue: RequirementRsValue.AsNumber): String = "%.2f".format(requirementValue.value)
        fun serialize(requirementValue: RequirementRsValue.AsInteger): Long = requirementValue.value
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        requirementValue: RequirementRsValue,
        jsonGenerator: JsonGenerator,
        provider: SerializerProvider
    ) =
        when (requirementValue) {
            is RequirementRsValue.AsString  -> jsonGenerator.writeString(serialize(requirementValue))
            is RequirementRsValue.AsNumber  -> jsonGenerator.writeNumber(serialize(requirementValue))
            is RequirementRsValue.AsBoolean -> jsonGenerator.writeBoolean(serialize(requirementValue))
            is RequirementRsValue.AsInteger -> jsonGenerator.writeNumber(serialize(requirementValue))
        }
}
