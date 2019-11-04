package com.procurement.procurer.infrastructure.model.dto.cn

import com.procurement.procurer.application.model.data.CheckCriteriaData
import com.procurement.procurer.application.model.data.CreateCriteriaData
import com.procurement.procurer.application.model.data.CreatedCriteria
import com.procurement.procurer.infrastructure.model.entity.CreatedCriteriaEntity

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

fun CreateCriteriaRequest.toData(): CreateCriteriaData {
    return CreateCriteriaData(
        tender = CreateCriteriaData.Tender(
            awardCriteria = this.tender.awardCriteria,
            awardCriteriaDetails = this.tender.awardCriteriaDetails,
            criteria = this.tender.criteria?.map { criteria ->
                CreateCriteriaData.Tender.Criteria(
                    id = criteria.id,
                    description = criteria.description,
                    title = criteria.title,
                    relatesTo = criteria.relatesTo,
                    relatedItem = criteria.relatedItem,
                    requirementGroups = criteria.requirementGroups.map { rg ->
                        CreateCriteriaData.Tender.Criteria.RequirementGroup(
                            id = rg.id,
                            description = rg.description,
                            requirements = rg.requirements
                        )
                    }
                )
            },
            conversions = this.tender.conversions?.map { conversion ->
                CreateCriteriaData.Tender.Conversion(
                    id = conversion.id,
                    relatedItem = conversion.relatedItem,
                    rationale = conversion.rationale,
                    coefficients = conversion.coefficients.map { coefficient ->
                        CreateCriteriaData.Tender.Conversion.Coefficient(
                            id = coefficient.id,
                            value = coefficient.value,
                            coefficient = coefficient.coefficient
                        )
                    },
                    relatesTo = conversion.relatesTo,
                    description = conversion.description
                )
            }
        )
    )
}

fun CheckResponsesRequest.toData(): CheckResponsesData {
    return CheckResponsesData(
        items = this.items.map { item ->
            CheckResponsesData.Item(
                id = item.id
            )
        },
        bid = CheckResponsesData.Bid(
            requirementResponses = this.bid.requirementResponses?.map { requirementResponse ->
                CheckResponsesData.Bid.RequirementResponse(
                    id = requirementResponse.id,
                    description = requirementResponse.description,
                    title = requirementResponse.title,
                    value = requirementResponse.value,
                    period = requirementResponse.period?.let { period ->
                        CheckResponsesData.Bid.RequirementResponse.Period(
                            startDate = period.startDate,
                            endDate = period.endDate
                        )
                    },
                    requirement = CheckResponsesData.Bid.RequirementResponse.Requirement(
                        id = requirementResponse.requirement.id
                    )
                )
            } ?: emptyList() ,
            relatedLots = this.bid.relatedLots
        )
    )
}

fun CreatedCriteriaEntity.toData(): CreatedCriteria {
    return CreatedCriteria(
        criteria = this.criteria?.map { criteria ->
            CreatedCriteria.Criteria(
                id = criteria.id,
                title = criteria.title,
                description = criteria.description,
                source = criteria.source,
                relatedItem = criteria.relatedItem,
                relatesTo = criteria.relatesTo,
                requirementGroups = criteria.requirementGroups.map { rg ->
                    CreatedCriteria.Criteria.RequirementGroup(
                        id = rg.id,
                        description = rg.description,
                        requirements = rg.requirements
                    )
                }
            )
        },
        conversions = this.conversions?.map { conversion ->
            CreatedCriteria.Conversion(
                id = conversion.id,
                relatesTo = conversion.relatesTo,
                relatedItem = conversion.relatedItem,
                description = conversion.description,
                rationale = conversion.rationale,
                coefficients = conversion.coefficients.map { coefficient ->
                    CreatedCriteria.Conversion.Coefficient(
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
