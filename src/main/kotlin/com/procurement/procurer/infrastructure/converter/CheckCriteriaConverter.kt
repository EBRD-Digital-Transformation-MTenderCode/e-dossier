package com.procurement.procurer.infrastructure.converter

import com.procurement.procurer.application.model.data.CheckCriteriaData
import com.procurement.procurer.infrastructure.model.dto.request.CheckCriteriaRequest

fun CheckCriteriaRequest.toData(): CheckCriteriaData {
    return CheckCriteriaData(
        items = this.items.map { item ->
            CheckCriteriaData.Tender.Item(
                id = item.id,
                relatedLot = item.relatedLot
            )
        },
        mainProcurementCategory = this.mainProcurementCategory,
        tender = CheckCriteriaData.Tender(
            awardCriteria = this.tender.awardCriteria,
            awardCriteriaDetails = this.tender.awardCriteriaDetails,
            criteria = this.tender.criteria?.map { criteria ->
                CheckCriteriaData.Tender.Criteria(
                    id = criteria.id,
                    description = criteria.description,
                    title = criteria.title,
                    relatesTo = criteria.relatesTo,
                    relatedItem = criteria.relatedItem,
                    requirementGroups = criteria.requirementGroups.map { rg ->
                        CheckCriteriaData.Tender.Criteria.RequirementGroup(
                            id = rg.id,
                            requirements = rg.requirements,
                            description = rg.description
                        )
                    }
                )
            },
            conversions = this.tender.conversions?.map { conversion ->
                CheckCriteriaData.Tender.Conversion(
                    id = conversion.id,
                    relatedItem = conversion.relatedItem,
                    rationale = conversion.rationale,
                    coefficients = conversion.coefficients.map { coefficient ->
                        CheckCriteriaData.Tender.Conversion.Coefficient(
                            id = coefficient.id,
                            value = coefficient.value,
                            coefficient = coefficient.coefficient
                        )
                    },
                    relatesTo = conversion.relatesTo,
                    description = conversion.description
                )
            },
            items = this.items.map { item ->
                CheckCriteriaData.Tender.Item(
                    id = item.id,
                    relatedLot = item.relatedLot
                )
            }
        )
    )
}
