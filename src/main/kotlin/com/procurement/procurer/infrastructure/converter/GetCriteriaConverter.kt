package com.procurement.procurer.infrastructure.converter

import com.procurement.procurer.application.model.data.CreatedCriteria
import com.procurement.procurer.application.model.data.GetCriteriaData
import com.procurement.procurer.infrastructure.model.dto.response.GetCriteriaResponse

fun createResponseData(createdCriteria: CreatedCriteria): GetCriteriaData {
    return GetCriteriaData(
        conversions = createdCriteria.conversions?.map { conversion ->
            GetCriteriaData.Conversion(
                id = conversion.id,
                relatesTo = conversion.relatesTo,
                relatedItem = conversion.relatedItem,
                description = conversion.description,
                rationale = conversion.rationale,
                coefficients = conversion.coefficients.map { coefficient ->
                    GetCriteriaData.Conversion.Coefficient(
                        id = coefficient.id,
                        value = coefficient.value,
                        coefficient = coefficient.coefficient
                    )
                }
            )
        },
        awardCriteria = createdCriteria.awardCriteria,
        awardCriteriaDetails = createdCriteria.awardCriteriaDetails!!
    )
}

fun GetCriteriaData.toResponseDto(): GetCriteriaResponse {
    return GetCriteriaResponse(
        conversions = this.conversions?.map { conversion ->
            GetCriteriaResponse.Conversion(
                id = conversion.id,
                relatesTo = conversion.relatesTo,
                relatedItem = conversion.relatedItem,
                description = conversion.description,
                rationale = conversion.rationale,
                coefficients = conversion.coefficients.map { coefficient ->
                    GetCriteriaResponse.Conversion.Coefficient(
                        id = coefficient.id,
                        value = coefficient.value,
                        coefficient = coefficient.coefficient
                    )
                }
            )
        },
        awardCriteria = this.awardCriteria,
        awardCriteriaDetails = this.awardCriteriaDetails
    )
}


