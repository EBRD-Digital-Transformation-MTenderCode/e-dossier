package com.procurement.procurer.infrastructure.converter

import com.procurement.procurer.application.model.data.RequestsForEvPanelsData
import com.procurement.procurer.application.model.data.Requirement
import com.procurement.procurer.infrastructure.model.dto.response.RequestsForEvPanelsResponse

fun RequestsForEvPanelsData.toResponseDto(): RequestsForEvPanelsResponse {
    return RequestsForEvPanelsResponse(
        criteria = this.criteria.let { criteria ->
            RequestsForEvPanelsResponse.Criteria(
                id = criteria.id,
                title = criteria.title,
                description = criteria.description,
                source = criteria.source,
                relatesTo = criteria.relatesTo,
                requirementGroups = criteria.requirementGroups.map { requirementGroup ->
                    RequestsForEvPanelsResponse.Criteria.RequirementGroup(
                        id = requirementGroup.id,
                        requirements = requirementGroup.requirements.map { requirement ->
                            Requirement(
                                id = requirement.id,
                                title = requirement.title,
                                dataType = requirement.dataType,
                                value = requirement.value,
                                period = requirement.period,
                                description = requirement.description
                            )
                        }
                    )
                }
            )
        }
    )
}
