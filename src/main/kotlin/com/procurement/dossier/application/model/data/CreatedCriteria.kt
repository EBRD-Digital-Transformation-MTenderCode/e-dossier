package com.procurement.dossier.application.model.data

import com.procurement.dossier.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.dossier.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.dossier.infrastructure.model.dto.ocds.ConversionsRelatesTo
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource

data class CreatedCriteria(
    val criteria: List<Criteria>?,
    val conversions: List<Conversion>?,
    val awardCriteria: AwardCriteria,
    val awardCriteriaDetails: AwardCriteriaDetails?
) {
    data class Criteria(
        val id: String,
        val title: String,
        val source: CriteriaSource?,
        val description: String?,
        val requirementGroups: List<RequirementGroup>,
        val relatesTo: CriteriaRelatesTo?,
        val relatedItem: String?
    ) {
        data class RequirementGroup(
            val id: String,
            val description: String?,
            val requirements: List<Requirement>
        )
    }

    data class Conversion(
        val id: String,
        val relatesTo: ConversionsRelatesTo,
        val relatedItem: String,
        val rationale: String,
        val description: String?,
        val coefficients: List<Coefficient>
    ) {
        data class Coefficient(
            val id: String,
            val value: CoefficientValue,
            val coefficient: CoefficientRate
        )
    }
}