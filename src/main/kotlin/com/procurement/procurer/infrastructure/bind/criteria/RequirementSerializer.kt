package com.procurement.procurer.infrastructure.bind.criteria

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.procurer.application.model.data.ExpectedValue
import com.procurement.procurer.application.model.data.MaxValue
import com.procurement.procurer.application.model.data.MinValue
import com.procurement.procurer.application.model.data.RangeValue
import com.procurement.procurer.application.model.data.Requirement
import java.io.IOException
import java.math.BigDecimal

class RequirementSerializer : JsonSerializer<List<Requirement>>() {
    companion object {
        fun serialize(requirements: List<Requirement>): ArrayNode {
            fun BigDecimal.jsonFormat() = BigDecimal("%.2f".format(this))

            val serializedRequirements = JsonNodeFactory.withExactBigDecimals(true).arrayNode()

            requirements.map { requirement ->
                val requirementNode = JsonNodeFactory.withExactBigDecimals(true).objectNode()

                requirementNode.put("id", requirement.id)
                requirementNode.put("title", requirement.title)
                requirementNode.put("dataType", requirement.dataType.value())

                requirement.description?.let { requirementNode.put("description", it) }

                requirement.period?.let {
                    requirementNode.putObject("period")
                        .put("startDate", JsonDateTimeSerializer.serialize(it.startDate))
                        .put("endDate", JsonDateTimeSerializer.serialize(it.endDate))
                }


                when (requirement.value) {
                    is ExpectedValue.AsString  -> {
                        requirementNode.put("expectedValue", requirement.value.value)
                    }
                    is ExpectedValue.AsBoolean -> {
                        requirementNode.put("expectedValue", requirement.value.value)
                    }
                    is ExpectedValue.AsNumber  -> {
                        requirementNode.put("expectedValue", requirement.value.value.jsonFormat())
                    }
                    is ExpectedValue.AsInteger -> {
                        requirementNode.put("expectedValue", requirement.value.value)
                    }
                    is RangeValue.AsNumber     -> {
                        requirementNode.put("minValue", requirement.value.minValue.jsonFormat())
                        requirementNode.put("maxValue", requirement.value.maxValue.jsonFormat())
                    }
                    is RangeValue.AsInteger    -> {
                        requirementNode.put("minValue", requirement.value.minValue)
                        requirementNode.put("maxValue", requirement.value.maxValue)
                    }
                    is MinValue.AsNumber       -> {
                        requirementNode.put("minValue", requirement.value.value.jsonFormat())
                    }
                    is MinValue.AsInteger      -> {
                        requirementNode.put("minValue", requirement.value.value)
                    }
                    is MaxValue.AsNumber       -> {
                        requirementNode.put("maxValue", requirement.value.value.jsonFormat())
                    }
                    is MaxValue.AsInteger      -> {
                        requirementNode.put("maxValue", requirement.value.value)
                    }
                }

                requirementNode
            }.also { it.forEach { requirement -> serializedRequirements.add(requirement) } }

            return serializedRequirements
        }
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        requirements: List<Requirement>,
        jsonGenerator: JsonGenerator,
        provider: SerializerProvider
    ) =
        jsonGenerator.writeTree(serialize(requirements))
}
