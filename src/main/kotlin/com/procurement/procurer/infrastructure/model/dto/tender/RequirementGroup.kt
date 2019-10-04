package com.procurement.procurer.infrastructure.model.dto.tender

data class RequirementGroup(
    val id: String,
    val requirements: List<Requirement>
)
