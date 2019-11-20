package com.procurement.procurer.infrastructure.converter

import com.procurement.procurer.application.model.data.CheckResponsesData
import com.procurement.procurer.infrastructure.model.dto.request.CheckResponsesRequest

fun CheckResponsesRequest.toData(): CheckResponsesData {
    return CheckResponsesData(
        items = this.items.map { item ->
            CheckResponsesData.Item(
                id = item.id
            )
        },
        bid = CheckResponsesData.Bid(
            requirementResponses = this.bid.requirementResponses?.map { requirementResponse ->
                CheckResponsesData.Bid.RequirementResponse(
                    id = requirementResponse.id,
                    description = requirementResponse.description,
                    title = requirementResponse.title,
                    value = requirementResponse.value,
                    period = requirementResponse.period?.let { period ->
                        CheckResponsesData.Bid.RequirementResponse.Period(
                            startDate = period.startDate,
                            endDate = period.endDate
                        )
                    },
                    requirement = CheckResponsesData.Bid.RequirementResponse.Requirement(
                        id = requirementResponse.requirement.id
                    )
                )
            } ?: emptyList(),
            relatedLots = this.bid.relatedLots
        )
    )
}