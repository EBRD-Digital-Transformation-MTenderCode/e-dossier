package com.procurement.procurer.infrastructure.model.dto.tender

import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaRelatesTo
import java.util.*

data class Criteria(
    val id: String,
    val requirementGroups: List<RequirementGroup>,
    val relatesTo: CriteriaRelatesTo?,
    val relatedItem: String?
)