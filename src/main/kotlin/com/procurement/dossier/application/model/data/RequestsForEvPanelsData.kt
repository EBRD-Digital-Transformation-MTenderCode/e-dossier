package com.procurement.procurer.application.model.data

import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaSource
import java.util.*

data class RequestsForEvPanelsData(
    val criteria: Criteria
) {
    data class Criteria(
        val id: UUID,
        val title: String,
        val source: CriteriaSource,
        val relatesTo: CriteriaRelatesTo,
        val description: String?,
        val requirementGroups: List<RequirementGroup>
    ) {
        data class RequirementGroup(
            val id: UUID,
            val requirements: List<Requirement>
        )
    }
}