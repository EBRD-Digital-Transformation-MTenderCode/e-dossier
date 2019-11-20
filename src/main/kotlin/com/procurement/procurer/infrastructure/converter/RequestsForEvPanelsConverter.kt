package com.procurement.procurer.infrastructure.converter

import com.procurement.procurer.application.model.data.RequestsForEvPanelsData
import com.procurement.procurer.application.model.data.Requirement
import com.procurement.procurer.infrastructure.model.dto.response.RequestsForEvPanelsResponse

fun RequestsForEvPanelsData.toResponseDto(): RequestsForEvPanelsResponse {
    return RequestsForEvPanelsResponse(
        criteria = RequestsForEvPanelsResponse.Criteria(
            id = this.criteria.id,
            title = this.criteria.title,
            description = this.criteria.description,
            source = this.criteria.source,
            requirementGroups = this.criteria.requirementGroups.map { requirementGroup ->
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
    )
}