package com.procurement.procurer.application.model.data

import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.model.dto.ocds.ConversionsRelatesTo
import java.math.BigDecimal

data class GetCriteriaData(
    val awardCriteria: AwardCriteria,
    val awardCriteriaDetails: AwardCriteriaDetails,
    val conversions: List<Conversion>?
) {
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