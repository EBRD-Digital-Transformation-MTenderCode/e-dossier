package com.procurement.dossier.infrastructure.converter

import com.procurement.dossier.application.model.data.CreatedCriteria
import com.procurement.dossier.application.model.data.RequestsForEvPanelsResult
import com.procurement.dossier.application.model.data.Requirement
import com.procurement.dossier.domain.model.criteria.CriteriaId
import com.procurement.dossier.domain.model.criteria.RequirementGroupId
import com.procurement.dossier.infrastructure.model.dto.response.RequestsForEvPanelsResponse

fun RequestsForEvPanelsResult.toResponseDto(): RequestsForEvPanelsResponse {
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

fun CreatedCriteria.Criteria.convert(): RequestsForEvPanelsResult {
    return RequestsForEvPanelsResult(
        criteria = this.let { criteria ->
            RequestsForEvPanelsResult.Criteria(
                id = CriteriaId.fromString(criteria.id),
                title = criteria.title,
                description = criteria.description,
                source = criteria.source!!,
                relatesTo = criteria.relatesTo!!,
                requirementGroups = criteria.requirementGroups.map { requirementGroup ->
                    RequestsForEvPanelsResult.Criteria.RequirementGroup(
                        id = RequirementGroupId.fromString(requirementGroup.id),
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
