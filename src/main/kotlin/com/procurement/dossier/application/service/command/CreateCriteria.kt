package com.procurement.dossier.application.service.command

import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.data.CreateCriteriaData
import com.procurement.dossier.application.model.data.CreatedCriteria
import com.procurement.dossier.application.model.data.Requirement
import com.procurement.dossier.application.model.entity.CnEntity
import com.procurement.dossier.application.service.Generable
import com.procurement.dossier.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.dossier.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.dossier.infrastructure.model.dto.ocds.ConversionsRelatesTo
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.dossier.infrastructure.model.dto.ocds.CriteriaSource
import com.procurement.dossier.infrastructure.model.dto.response.CreateCriteriaResponse
import com.procurement.dossier.infrastructure.model.entity.CreatedCriteriaEntity
import com.procurement.dossier.infrastructure.utils.toJson

fun buildCriteria(
    data: CreateCriteriaData,
    generationService: Generable
): CreatedCriteria {
    fun replaceConversionRelation(
        conversion: CreateCriteriaData.Tender.Conversion,
        relations: Map<String, String>
    ): String {
        if (conversion.relatesTo == ConversionsRelatesTo.REQUIREMENT) return relations.get(conversion.relatedItem)
            ?: throw ErrorException(
                ErrorType.INVALID_CONVERSION,
                message = "Conversion relates to requirement that does not exists. " +
                    "Conversion.id=${conversion.id}, Conversion.relatedItem=${conversion.relatedItem}"
            )
        return conversion.relatedItem
    }

    val requirementTempToPermanentIdRelation = mutableMapOf<String, String>()

    return CreatedCriteria(
        criteria = data.tender.criteria?.map { criteria ->
            CreatedCriteria.Criteria(
                id = generationService.generatePermanentCriteriaId().toString(),
                title = criteria.title,
                description = criteria.description,
                relatesTo = criteria.relatesTo,
                relatedItem = criteria.relatedItem,
                source = defineSource(criteria),
                requirementGroups = criteria.requirementGroups.map { rg ->
                    CreatedCriteria.Criteria.RequirementGroup(
                        id = generationService.generatePermanentRequirementGroupId().toString(),
                        description = rg.description,
                        requirements = rg.requirements.map { requirement ->
                            val permanentId = generationService.generatePermanentRequirementId().toString()
                            requirementTempToPermanentIdRelation.put(requirement.id, permanentId)
                            Requirement(
                                id = permanentId,
                                title = requirement.title,
                                description = requirement.description,
                                period = requirement.period,
                                dataType = requirement.dataType,
                                value = requirement.value
                            )
                        }
                    )
                }
            )
        },
        conversions = data.tender.conversions?.map { conversion ->
            CreatedCriteria.Conversion(
                id = generationService.generatePermanentConversionId().toString(),
                description = conversion.description,
                relatesTo = conversion.relatesTo,
                relatedItem = replaceConversionRelation(conversion, requirementTempToPermanentIdRelation),
                rationale = conversion.rationale,
                coefficients = conversion.coefficients.map { coefficient ->
                    CreatedCriteria.Conversion.Coefficient(
                        id = generationService.generatePermanentCoefficientId().toString(),
                        value = coefficient.value,
                        coefficient = coefficient.coefficient
                    )
                }

            )
        },
        awardCriteria = data.tender.awardCriteria,
        awardCriteriaDetails = setAwardCriteriaDetails(data.tender.awardCriteria) ?: data.tender.awardCriteriaDetails
    )
}

fun CreatedCriteria.toEntity(): CreatedCriteriaEntity {
    return CreatedCriteriaEntity(
        criteria = this.criteria?.map { criteria ->
            CreatedCriteriaEntity.Criteria(
                id = criteria.id,
                title = criteria.title,
                description = criteria.description,
                source = criteria.source,
                relatedItem = criteria.relatedItem,
                relatesTo = criteria.relatesTo,
                requirementGroups = criteria.requirementGroups.map { rg ->
                    CreatedCriteriaEntity.Criteria.RequirementGroup(
                        id = rg.id,
                        description = rg.description,
                        requirements = rg.requirements
                    )
                }
            )
        },
        conversions = this.conversions?.map { conversion ->
            CreatedCriteriaEntity.Conversion(
                id = conversion.id,
                relatesTo = conversion.relatesTo,
                relatedItem = conversion.relatedItem,
                description = conversion.description,
                rationale = conversion.rationale,
                coefficients = conversion.coefficients.map { coefficient ->
                    CreatedCriteriaEntity.Conversion.Coefficient(
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

private fun defineSource(criteria: CreateCriteriaData.Tender.Criteria): CriteriaSource? =
    if (criteria.relatesTo == null || criteria.relatesTo != CriteriaRelatesTo.TENDERER) CriteriaSource.TENDERER else null

private fun setAwardCriteriaDetails(awardCriteria: AwardCriteria): AwardCriteriaDetails? =
    if (awardCriteria == AwardCriteria.PRICE_ONLY) AwardCriteriaDetails.AUTOMATED else null

fun createCnEntity(entity: CreatedCriteriaEntity, cpid: String, owner: String): CnEntity {
    return CnEntity(
        cpid = cpid,
        owner = owner,
        jsonData = toJson(entity)
    )
}

fun generateCreateCriteriaResponse(createdCriteria: CreatedCriteria) =
    CreateCriteriaResponse(
        criteria = createdCriteria.criteria?.map { criteria ->
            CreateCriteriaResponse.Criteria(
                id = criteria.id,
                title = criteria.title,
                description = criteria.description,
                relatedItem = criteria.relatedItem,
                relatesTo = criteria.relatesTo,
                source = criteria.source,
                requirementGroups = criteria.requirementGroups.map { rg ->
                    CreateCriteriaResponse.Criteria.RequirementGroup(
                        id = rg.id,
                        description = rg.description,
                        requirements = rg.requirements.map { requirement ->
                            Requirement(
                                id = requirement.id,
                                title = requirement.title,
                                description = requirement.description,
                                period = requirement.period,
                                dataType = requirement.dataType,
                                value = requirement.value
                            )
                        }
                    )
                }
            )
        },
        conversions = createdCriteria.conversions?.map { conversion ->
            CreateCriteriaResponse.Conversion(
                id = conversion.id,
                relatesTo = conversion.relatesTo,
                relatedItem = conversion.relatedItem,
                rationale = conversion.rationale,
                description = conversion.description,
                coefficients = conversion.coefficients.map { coefficient ->
                    CreateCriteriaResponse.Conversion.Coefficient(
                        id = coefficient.id,
                        value = coefficient.value,
                        coefficient = coefficient.coefficient
                    )
                }
            )
        },
        awardCriteriaDetails = createdCriteria.awardCriteriaDetails
    )