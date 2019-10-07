package com.procurement.procurer.infrastructure.bind.criteria

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.procurer.infrastructure.model.dto.data.ExpectedValue
import com.procurement.procurer.infrastructure.model.dto.data.MaxValue
import com.procurement.procurer.infrastructure.model.dto.data.MinValue
import com.procurement.procurer.infrastructure.model.dto.data.RangeValue
import com.procurement.procurer.infrastructure.model.dto.data.Requirement
import java.io.IOException

class RequirementSerializer : JsonSerializer<List<Requirement>>() {
    companion object {
        fun serialize(requirements: List<Requirement>): ArrayNode {
            val serializedRequirements = JsonNodeFactory.instance.arrayNode()

            requirements.map { requirement ->
                val requirementNode = JsonNodeFactory.instance.objectNode()

                requirementNode.put("id", requirement.id)
                requirementNode.put("title", requirement.title)
                requirementNode.put("dataType", requirement.dataType.value())

                requirement.description?.let {  requirementNode.put("description", it) }

                requirement.period?.let {
                    requirementNode.putObject("period")
                        .put("startDate", JsonDateTimeSerializer.serialize(it.startDate))
                        .put("endDate", JsonDateTimeSerializer.serialize(it.endDate))
                }


                when (requirement.value) {
                    is ExpectedValue -> {
                        when (requirement.value) {
                            is ExpectedValue.AsString  -> {
                                requirementNode.put("expectedValue", requirement.value.value)
                            }
                            is ExpectedValue.AsBoolean -> {
                                requirementNode.put("expectedValue", requirement.value.value)
                            }
                            is ExpectedValue.AsNumber  -> {
                                requirementNode.put("expectedValue", requirement.value.value)
                            }
                            is ExpectedValue.AsInteger -> {
                                requirementNode.put("expectedValue", requirement.value.value)
                            }
                        }
                    }
                    is RangeValue    -> when (requirement.value) {
                        is RangeValue.AsNumber  -> {
                            requirementNode.put("minValue", requirement.value.minValue)
                            requirementNode.put("maxValue", requirement.value.maxValue)
                        }
                        is RangeValue.AsInteger -> {
                            requirementNode.put("minValue", requirement.value.minValue)
                            requirementNode.put("maxValue", requirement.value.maxValue)
                        }
                    }
                    is MinValue      -> when (requirement.value) {
                        is MinValue.AsNumber  -> {
                            requirementNode.put("minValue", requirement.value.value)
                        }
                        is MinValue.AsInteger -> {
                            requirementNode.put("minValue", requirement.value.value)
                        }
                    }
                    is MaxValue      -> when (requirement.value) {
                        is MaxValue.AsNumber  -> {
                            requirementNode.put("maxValue", requirement.value.value)
                        }
                        is MaxValue.AsInteger -> {
                            requirementNode.put("maxValue", requirement.value.value)
                        }
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
