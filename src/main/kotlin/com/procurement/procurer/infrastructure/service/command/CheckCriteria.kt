package com.procurement.procurer.infrastructure.service.command

import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.application.model.data.CheckCriteriaData
import com.procurement.procurer.application.model.data.CoefficientValue
import com.procurement.procurer.application.model.data.ExpectedValue
import com.procurement.procurer.application.model.data.MaxValue
import com.procurement.procurer.application.model.data.MinValue
import com.procurement.procurer.application.model.data.Period
import com.procurement.procurer.application.model.data.RangeValue
import com.procurement.procurer.application.model.data.Requirement
import com.procurement.procurer.application.model.data.RequirementValue
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.model.dto.ocds.ConversionsRelatesTo
import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.procurer.infrastructure.model.dto.ocds.MainProcurementCategory
import com.procurement.procurer.infrastructure.model.dto.ocds.RequirementDataType
import java.math.BigDecimal
import java.util.*

fun CheckCriteriaData.checkConversionWithoutCriteria(): CheckCriteriaData {
    val tender = this.tender
    if (tender.criteria == null && tender.conversions != null)
        throw ErrorException(
            ErrorType.INVALID_CONVERSION,
            message = "Conversions cannot exists without criteria"
        )
    return this
}

fun CheckCriteriaData.checkActualItemRelation(): CheckCriteriaData {
    fun CheckCriteriaData.Tender.Criteria.hasRelation(): Boolean = this.relatesTo != null && this.relatedItem != null

    fun Map<String, CheckCriteriaData.Tender.Item>.containsElement(itemId: String) {
        if (!this.containsKey(itemId)) throw ErrorException(
            ErrorType.INVALID_CRITERIA,
            message = "Criteria relates to item that does not exists. Item id=${itemId}, Available items: ${this.keys}"
        )
    }

    fun Map<String, List<CheckCriteriaData.Tender.Item>>.relatesWithLot(lotId: String) {
        if (!this.containsKey(lotId)) throw ErrorException(
            ErrorType.INVALID_CRITERIA,
            message = "Criteria relates to lot that does not exists. Item id=${lotId}, Available lots: ${this.keys}"
        )
    }

    fun CheckCriteriaData.Tender.Criteria.validate(
        itemsById: Map<String, CheckCriteriaData.Tender.Item>,
        itemsByRelatedLot: Map<String, List<CheckCriteriaData.Tender.Item>>
    ) {
        when (this.relatesTo) {
            CriteriaRelatesTo.ITEM     -> itemsById.containsElement(this.relatedItem!!)
            CriteriaRelatesTo.LOT      -> itemsByRelatedLot.relatesWithLot(this.relatedItem!!)
            CriteriaRelatesTo.TENDERER -> Unit
        }
    }

    val criteria = this.tender.criteria ?: return this
    val items = this.tender.items

    val itemsById = items.associateBy { it.id }
    val itemsByRelatedLot = items.groupBy { it.relatedLot }

    criteria
        .filter { _criteria -> _criteria.hasRelation() }
        .forEach { _criteria -> _criteria.validate(itemsById, itemsByRelatedLot) }

    return this
}

fun CheckCriteriaData.checkDatatypeCompliance(): CheckCriteriaData {
    fun mismatchDatatypeException(rv: RequirementValue?, rDatatype: RequirementDataType): Nothing =
        throw ErrorException(
            ErrorType.INVALID_CRITERIA,
            message = "Requirement.dataType mismatch with datatype in expectedValue || minValue || maxValue. " +
                " \n${rv} != ${rDatatype}"
        )

    fun Requirement.hasRequirementValue(): Boolean = this.value != null
    fun Requirement.validate() {
        if (!this.hasRequirementValue()) return

        when (this.value) {
            is ExpectedValue.AsBoolean -> if (this.dataType != RequirementDataType.BOOLEAN)
                mismatchDatatypeException(this.value, this.dataType)

            is ExpectedValue.AsString  -> if (this.dataType != RequirementDataType.STRING)
                mismatchDatatypeException(this.value, this.dataType)

            is ExpectedValue.AsInteger,
            is MinValue.AsInteger,
            is MaxValue.AsInteger,
            is RangeValue.AsInteger    -> if (this.dataType != RequirementDataType.INTEGER)
                mismatchDatatypeException(this.value, this.dataType)

            is ExpectedValue.AsNumber,
            is MinValue.AsNumber,
            is MaxValue.AsNumber,
            is RangeValue.AsNumber     -> if (this.dataType != RequirementDataType.NUMBER)
                mismatchDatatypeException(this.value, this.dataType)
        }
    }

    val criteria = this.tender.criteria ?: return this

    criteria.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .forEach { it.validate() }

    return this
}

fun CheckCriteriaData.checkMinMaxValue(): CheckCriteriaData {
    fun rangeException(): Nothing = throw ErrorException(
        ErrorType.INVALID_REQUIREMENT_VALUE,
        message = "minValue greater than maxValue"
    )

    fun <T : Number> validateRange(minValue: T, maxValue: T) {
        when (minValue) {
            is Long       -> if (minValue > maxValue.toLong()) rangeException()
            is BigDecimal -> if (minValue > BigDecimal(maxValue.toString())) rangeException()
        }
    }

    fun RangeValue.validate() {
        when (this) {
            is RangeValue.AsNumber  -> validateRange(minValue = this.minValue, maxValue = this.maxValue)
            is RangeValue.AsInteger -> validateRange(minValue = this.minValue, maxValue = this.maxValue)
        }
    }

    val criteria = this.tender.criteria ?: return this

    criteria.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .forEach { if (it.value is RangeValue) it.value.validate() }

    return this
}

fun CheckCriteriaData.checkDateTime(): CheckCriteriaData {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    fun Period.validate() {
        if (this.startDate.year > currentYear || this.endDate.year > currentYear)
            throw ErrorException(
                ErrorType.INVALID_PERIOD_VALUE,
                message = "start/endDate year cannot be greater than current year. " +
                    "StartDate=${this.startDate}, " +
                    "EndDate=${this.endDate}, " +
                    "Current year = ${currentYear}"
            )
        if (this.startDate > this.endDate)
            throw ErrorException(
                ErrorType.INVALID_PERIOD_VALUE,
                message = "startDate cannot be greater than endDate. StartDate=${this.startDate}, EndDate=${this.endDate}"
            )
    }

    val criteria = this.tender.criteria ?: return this

    criteria.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .forEach { it.period?.validate() }

    return this
}

fun CheckCriteriaData.checkRequirements(): CheckCriteriaData {

    val criteria = this.tender.criteria ?: return this

    val criteriaById = criteria.groupBy { it.id }
    val requirementGroupById = criteria.flatMap { it.requirementGroups }
        .groupBy { it.id }
    val requirementById = criteria.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .groupBy { it.id }

    fun CheckCriteriaData.Tender.Criteria.validateId() {
        val criteriaId = criteriaById.get(this.id)
        if (criteriaId != null && criteriaId.size > 1)
            throw ErrorException(
                ErrorType.INVALID_CRITERIA,
                message = "criteria.id is not unique. Current id = ${this.id}, All ids: ${criteriaById}"
            )
    }

    fun CheckCriteriaData.Tender.Criteria.RequirementGroup.validateId() {
        val requirementGroup = requirementGroupById.get(this.id)
        if (requirementGroup != null && requirementGroup.size > 1)
            throw ErrorException(
                ErrorType.INVALID_CRITERIA,
                message = "requirementGroup.id is not unique. Current id = ${this.id}. All ids: ${requirementGroupById}"
            )
    }

    fun Requirement.validateId() {
        val requirement = requirementById.get(this.id)
        if (requirement != null && requirement.size > 1)
            throw ErrorException(
                ErrorType.INVALID_CRITERIA,
                message = "requirement.id is not unique. Current id = ${this.id}. All ids: ${requirementById}"
            )
    }

    fun List<CheckCriteriaData.Tender.Criteria.RequirementGroup>.validateRequirementGroupCount() {
        if (this.size == 0)
            throw ErrorException(
                ErrorType.INVALID_CRITERIA,
                message = "Must be at least one requirementGroup have to be added"
            )
    }

    fun List<Requirement>.validateRequirementsCount() {
        if (this.size == 0)
            throw ErrorException(
                ErrorType.INVALID_CRITERIA,
                message = "Must be at least one requirements have to be added"
            )
    }


    criteria.forEach {
        it.validateId()
        it.requirementGroups.validateRequirementGroupCount()

        it.requirementGroups.forEach { rg ->
            rg.validateId()
            rg.requirements.validateRequirementsCount()
            rg.requirements.forEach { requirement ->
                requirement.validateId()
            }
        }
    }


    return this
}

fun CheckCriteriaData.checkConversionRelation(): CheckCriteriaData {

    val criteria = this.tender.criteria ?: return this
    val conversions = this.tender.conversions ?: return this

    val requirements = criteria.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .groupBy { it.id }

    val relation: MutableMap<String, List<Requirement>> = mutableMapOf()
    relation.putAll(requirements)

    val conversionsRelatesToRequirement = conversions.filter { it.relatesTo == ConversionsRelatesTo.REQUIREMENT }

    conversionsRelatesToRequirement.map {
        if (!requirements.containsKey(it.relatedItem)) throw ErrorException(
            ErrorType.INVALID_CONVERSION,
            message = "Conversion relates to requirement that does not exists"
        )

        if (!relation.containsKey(it.relatedItem)) throw ErrorException(
            ErrorType.INVALID_CONVERSION,
            message = "Conversion relates to requirement that already in relation with another conversion"
        )
        relation.remove(it.relatedItem)
    }

    return this
}

fun CheckCriteriaData.checkCoefficient(): CheckCriteriaData {
    val COEFFICIENT_MIN = 0.01.toBigDecimal()
    val COEFFICIENT_MAX = 2.toBigDecimal()

    fun CheckCriteriaData.Tender.Conversion.Coefficient.validateCoefficientRate() {
        if (this.coefficient < COEFFICIENT_MIN || this.coefficient > COEFFICIENT_MAX)
            throw ErrorException(
                ErrorType.INVALID_CONVERSION,
                message = "Conversion coefficient rate (${this.coefficient}) does not satisfy the conditions: " +
                    "coefficient in [${COEFFICIENT_MIN}, ${COEFFICIENT_MAX}]"
            )
    }

    val conversions = this.tender.conversions ?: return this

    conversions
        .flatMap { it.coefficients }
        .forEach { it.validateCoefficientRate() }

    return this
}

fun CheckCriteriaData.checkCoefficientDataType(): CheckCriteriaData {
    fun mismatchDataTypeException(cv: CoefficientValue, rv: RequirementValue): Nothing =
        throw ErrorException(
            ErrorType.INVALID_CONVERSION,
            message = "DataType in Conversion mismatch with Requirement dataType. " +
                "\n ${cv} != ${rv}"
        )

    fun RequirementValue.validateDataType(coefficient: CheckCriteriaData.Tender.Conversion.Coefficient) {
        when (coefficient.value) {
            is CoefficientValue.AsBoolean -> if (this !is ExpectedValue.AsBoolean)
                mismatchDataTypeException(coefficient.value, this)

            is CoefficientValue.AsString  -> if (this !is ExpectedValue.AsString)
                mismatchDataTypeException(coefficient.value, this)

            is CoefficientValue.AsNumber  -> if (this !is ExpectedValue.AsNumber
                && this !is RangeValue.AsNumber
                && this !is MinValue.AsNumber
                && this !is MaxValue.AsNumber
            ) mismatchDataTypeException(coefficient.value, this)

            is CoefficientValue.AsInteger -> if (this !is ExpectedValue.AsInteger
                && this !is RangeValue.AsInteger
                && this !is MinValue.AsInteger
                && this !is MaxValue.AsInteger
            ) mismatchDataTypeException(coefficient.value, this)
        }
    }

    val criteria = this.tender.criteria ?: return this
    val conversions = this.tender.conversions ?: return this

    val requirements = criteria.asSequence()
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .associateBy { it.id }

    val conversionsRelatesToRequirement = conversions.filter { it.relatesTo == ConversionsRelatesTo.REQUIREMENT }

    conversionsRelatesToRequirement.forEach {
        val requirementId = it.relatedItem
        it.coefficients.forEach { coefficient ->
            val requirement = requirements.get(requirementId)
            requirement?.value?.validateDataType(coefficient)
        }
    }

    return this
}

fun CheckCriteriaData.checkCastCoefficient(): CheckCriteriaData {
    fun castCoefficientException(limit: BigDecimal): Nothing =
        throw ErrorException(
            ErrorType.INVALID_CONVERSION,
            message = "cast coefficient in conversion cannot be greater than ${limit} "
        )

    val criteria: List<CheckCriteriaData.Tender.Criteria> = this.tender.criteria ?: return this

    val conversions: List<CheckCriteriaData.Tender.Conversion> = this.tender.conversions!!.filter { it.relatesTo == ConversionsRelatesTo.REQUIREMENT }
    val items: List<CheckCriteriaData.Tender.Item> = this.tender.items

    val tenderRequirements = criteria.asSequence()
        .filter { it.relatesTo == null }
        .flatMap { it.requirementGroups.asSequence() }
        .flatMap { it.requirements.asSequence() }
        .toList()

    val tenderConversions = tenderRequirements.flatMap { requirement ->
        conversions.filter { it.relatedItem == requirement.id }
    }

    fun CheckCriteriaData.Tender.Criteria.getRelatedItems(items: List<CheckCriteriaData.Tender.Item>) =
        items.filter { it.relatedLot == this.relatedItem }

    val criteriaRelatedToLot = criteria.filter { it.relatesTo == CriteriaRelatesTo.LOT }
    val criteriaRelatedToItem = criteria.filter { it.relatesTo == CriteriaRelatesTo.ITEM }

    criteriaRelatedToLot.forEach { lotCriteria ->
        val lotRequirement = lotCriteria.requirementGroups
            .flatMap { it.requirements }
        val lotConversions = lotRequirement.flatMap { requirement ->
            conversions.filter { it.relatedItem == requirement.id }
        }

        val relatedItems = lotCriteria.getRelatedItems(items)

        val itemRequirement = relatedItems.flatMap { item ->
            criteriaRelatedToItem.asSequence()
                .filter { it.relatedItem == item.id }
                .flatMap { it.requirementGroups.asSequence() }
                .flatMap { it.requirements.asSequence() }
                .toList()
        }
        val itemConversions = itemRequirement.flatMap { requirement ->
            conversions.filter { it.relatedItem == requirement.id }
        }

        val castCoefficient = (tenderConversions + lotConversions + itemConversions)
            .map { conversion -> BigDecimal(1) - conversion.coefficients.minBy { it.coefficient }!!.coefficient }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        val mainProcurementCategory = this.tender.mainProcurementCategory

        val MAX_LIMIT_FOR_GOODS = 0.6.toBigDecimal()
        val MAX_LIMIT_FOR_WORKS = 0.8.toBigDecimal()
        val MAX_LIMIT_FOR_SERVICES = 0.4.toBigDecimal()

        when (mainProcurementCategory) {
            MainProcurementCategory.GOODS    -> if (castCoefficient > MAX_LIMIT_FOR_GOODS)
                castCoefficientException(MAX_LIMIT_FOR_GOODS)

            MainProcurementCategory.WORKS    -> if (castCoefficient > MAX_LIMIT_FOR_WORKS)
                castCoefficientException(MAX_LIMIT_FOR_WORKS)

            MainProcurementCategory.SERVICES -> if (castCoefficient > MAX_LIMIT_FOR_SERVICES)
                castCoefficientException(MAX_LIMIT_FOR_SERVICES)
        }
    }

    return this
}

fun CheckCriteriaData.checkConversionRelatesToEnum(): CheckCriteriaData {
    fun CheckCriteriaData.Tender.Conversion.validate() {
        when (this.relatesTo) {
            ConversionsRelatesTo.REQUIREMENT,
            ConversionsRelatesTo.OBSERVATION,
            ConversionsRelatesTo.OPTION -> Unit
        }
    }

    val tender = this.tender

    val conversions = tender.conversions ?: return this

    conversions.forEach { it.validate() }

    return this
}

fun CheckCriteriaData.checkAwardCriteriaEnum(): CheckCriteriaData {
    val tender = this.tender
    when (tender.awardCriteria) {
        AwardCriteria.PRICE_ONLY,
        AwardCriteria.COST_ONLY,
        AwardCriteria.QUALITY_ONLY,
        AwardCriteria.RATED_CRITERIA -> Unit
    }

    return this
}

fun CheckCriteriaData.checkAwardCriteriaDetailsEnum(): CheckCriteriaData {
    when (this.tender.awardCriteriaDetails) {
        AwardCriteriaDetails.MANUAL,
        AwardCriteriaDetails.AUTOMATED -> Unit
    }

    return this
}

fun CheckCriteriaData.checkArrays(): CheckCriteriaData {
    fun List<Any>.validate() {
        if (this.size < 1) throw ErrorException(
            ErrorType.INVALID_CONVERSION,
            message = "All arrays in json must have at least one object have to be added "
        )
    }

    val tender = this.tender

    tender.items.validate()

    tender.criteria?.validate()
    tender.criteria?.forEach { it.requirementGroups.validate() }
    tender.criteria?.forEach { it.requirementGroups.forEach { it.requirements.validate() } }

    tender.conversions?.validate()
    tender.conversions?.forEach { it.coefficients.validate() }

    return this
}

fun CheckCriteriaData.checkAwardCriteriaDetailsAreRequired(): CheckCriteriaData {

    val tender = this.tender

    when (tender.awardCriteria) {
        AwardCriteria.COST_ONLY,
        AwardCriteria.QUALITY_ONLY,
        AwardCriteria.RATED_CRITERIA -> if (tender.awardCriteriaDetails == null)
            throw ErrorException(
                ErrorType.INVALID_AWARD_CRITERIA,
                message = "For awardCriteria in [" +
                    "${AwardCriteria.COST_ONLY}, " +
                    "${AwardCriteria.QUALITY_ONLY}, " +
                    "${AwardCriteria.RATED_CRITERIA}" +
                    "] field 'awardCriteriaDetails' are required "
            )
        AwardCriteria.PRICE_ONLY     -> Unit
    }

    return this
}

fun CheckCriteriaData.checkCriteriaAndConversionAreRequired(): CheckCriteriaData {

    val tender = this.tender

    fun isNonPriceCriteria() = when (tender.awardCriteria) {
        AwardCriteria.COST_ONLY,
        AwardCriteria.QUALITY_ONLY,
        AwardCriteria.RATED_CRITERIA -> true
        AwardCriteria.PRICE_ONLY     -> false
    }

    fun isAutomatedCriteria() = when (tender.awardCriteriaDetails) {
        AwardCriteriaDetails.AUTOMATED -> true
        AwardCriteriaDetails.MANUAL    -> false
        null                           -> false
    }

    if (isNonPriceCriteria()
        && isAutomatedCriteria()
        && (tender.criteria == null || tender.conversions == null)
    ) throw ErrorException(
        ErrorType.INVALID_AWARD_CRITERIA,
        message = "For awardCriteria in [" +
            "${AwardCriteria.COST_ONLY}, " +
            "${AwardCriteria.QUALITY_ONLY}, " +
            "${AwardCriteria.RATED_CRITERIA}" +
            "] && 'awardCriteriaDetails' in [${AwardCriteriaDetails.AUTOMATED} Criteria and Conversion are required] ' "
    )

    return this
}