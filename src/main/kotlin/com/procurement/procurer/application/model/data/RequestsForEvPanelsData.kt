package com.procurement.procurer.application.model.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaSource
import java.util.*

data class RequestsForEvPanelsData(
    val criteria: Criteria
) {
    data class Criteria(
        val id: UUID,
        val title: String,
        val source: CriteriaSource,
        val description: String?,
        val requirementGroups: List<RequirementGroup>
    ) {
        data class RequirementGroup(
            val id: UUID,
            val requirements: List<Requirement>
        )
    }
}