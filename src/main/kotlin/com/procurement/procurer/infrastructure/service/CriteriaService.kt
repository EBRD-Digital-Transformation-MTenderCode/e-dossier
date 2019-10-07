package com.procurement.procurer.infrastructure.service

import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.procurer.infrastructure.model.dto.bpe.ResponseDto
import com.procurement.procurer.infrastructure.model.dto.cn.CheckCriteriaRequest
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender.Conversion
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender.Conversion.Coefficient
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender.Criteria
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender.Criteria.RequirementGroup
import com.procurement.procurer.infrastructure.model.dto.data.CheckCriteriaData.Tender.Item
import com.procurement.procurer.infrastructure.model.dto.data.CoefficientValue
import com.procurement.procurer.infrastructure.model.dto.data.ExpectedValue
import com.procurement.procurer.infrastructure.model.dto.data.MaxValue
import com.procurement.procurer.infrastructure.model.dto.data.MinValue
import com.procurement.procurer.infrastructure.model.dto.data.Period
import com.procurement.procurer.infrastructure.model.dto.data.RangeValue
import com.procurement.procurer.infrastructure.model.dto.data.Requirement
import com.procurement.procurer.infrastructure.model.dto.data.RequirementValue
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.model.dto.ocds.ConversionsRelatesTo
import com.procurement.procurer.infrastructure.model.dto.ocds.CriteriaRelatesTo
import com.procurement.procurer.infrastructure.model.dto.ocds.MainProcurementCategory
import com.procurement.procurer.infrastructure.model.dto.ocds.RequirementDataType
import com.procurement.procurer.infrastructure.utils.toObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class CriteriaService {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CriteriaService::class.java)
    }

    fun createCriteria(cm: CommandMessage): ResponseDto {
        val request: CheckCriteriaRequest = toObject(
            CheckCriteriaRequest::class.java,
            cm.data
        )
        val tender = convertDtoTender(request.tender)



        return ResponseDto(data = "ok")
    }

    fun checkCriteria(cm: CommandMessage): ResponseDto {
        val request: CheckCriteriaRequest = toObject(
            CheckCriteriaRequest::class.java,
            cm.data
        )
        val tender = convertDtoTender(request.tender).tender

        if (tender.criteria == null && tender.conversions != null)
            throw ErrorException(
                ErrorType.INVALID_CONVERSION,
                message = "Conversions cannot exists without criteria"
            )

        // FReq-1.1.1.22
        checkAwardCriteriaDetailsAreRequired(tender)

        // FReq-1.1.1.23
        checkCriteriaAndConversionAreRequired(tender)


        tender.criteria?.let { criteria ->
            // FReq-1.1.1.3
            checkActualItemRelation(criteria, tender.items)

            // FReq-1.1.1.4
            checkDatatypeCompliance(criteria)

            // FReq-1.1.1.5
            checkMinMaxValue(criteria)

            // FReq-1.1.1.6
            checkDateTime(criteria)

            // FReq-1.1.1.8
            checkRequirements(criteria)

            tender.conversions?.let { conversions ->
                // FReq-1.1.1.9
                // FReq-1.1.1.10
                checkConversionRelation(criteria, conversions)

                // FReq-1.1.1.11
                checkCoefficient(conversions)

                // FReq-1.1.1.12
                checkCoefficientDataType(criteria, conversions)

                // FReq-1.1.1.13
                checkCastCoefficient(tender)

                // FReq-1.1.1.14
                checkConversionRelatesToEnum(conversions)

                // FReq-1.1.1.15
                checkAwardCriteriaEnum(tender.awardCriteria)
                tender.awardCriteriaDetails?.let { checkAwardCriteriaDetailsEnum(it) }

                // FReq-1.1.1.16
                checkArrays(tender)
            }
        }


        return ResponseDto(data = "ok")
    }

    private fun checkActualItemRelation(criteria: List<Criteria>, items: List<Item>) {
        fun Criteria.hasRelation(): Boolean = this.relatesTo != null && this.relatedItem != null

        fun Map<String, Item>.containsElement(itemId: String) {
            if (!this.containsKey(itemId)) throw ErrorException(
                ErrorType.INVALID_CRITERIA,
                message = "Criteria relates to item that does not exists"
            )
        }

        fun Map<String, List<Item>>.relatesWithLot(lotId: String) {
            if (!this.containsKey(lotId)) throw ErrorException(
                ErrorType.INVALID_CRITERIA,
                message = "Criteria relates to lot that does not exists"
            )
        }

        fun Criteria.validate(itemsById: Map<String, Item>, itemsByRelatedLot: Map<String, List<Item>>) {
            when (this.relatesTo) {
                CriteriaRelatesTo.ITEM     -> itemsById.containsElement(this.relatedItem!!)
                CriteriaRelatesTo.LOT      -> itemsByRelatedLot.relatesWithLot(this.relatedItem!!)
                CriteriaRelatesTo.TENDERER -> Unit
            }
        }

        val itemsById = items.associateBy { it.id }
        val itemsByRelatedLot = items.groupBy { it.relatedLot }

        criteria
            .filter { _criteria -> _criteria.hasRelation() }
            .forEach { _criteria -> _criteria.validate(itemsById, itemsByRelatedLot) }
    }

    private fun checkDatatypeCompliance(criteria: List<Criteria>) {
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

        criteria
            .flatMap { it.requirementGroups }
            .flatMap { it.requirements }
            .forEach { it.validate() }
    }

    private fun checkMinMaxValue(criteria: List<Criteria>) {
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

        criteria
            .flatMap { it.requirementGroups }
            .flatMap { it.requirements }
            .forEach { if (it.value is RangeValue) it.value.validate() }
    }

    private fun checkDateTime(criteria: List<Criteria>) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        fun Period.validate() {
            if (this.startDate.year > currentYear || this.endDate.year > currentYear)
                throw ErrorException(
                    ErrorType.INVALID_PERIOD_VALUE,
                    message = "start/endDate year cannot be greater than current year"
                )
            if (this.startDate > this.endDate)
                throw ErrorException(
                    ErrorType.INVALID_PERIOD_VALUE,
                    message = "startDate cannot be greater than endDate"
                )
        }

        criteria
            .flatMap { it.requirementGroups }
            .flatMap { it.requirements }
            .forEach { it.period?.validate() }
    }

    private fun checkRequirements(criteria: List<Criteria>) {

        val criteriaById = criteria.groupBy { it.id }
        val requirementGroupById = criteria.flatMap { it.requirementGroups }
            .groupBy { it.id }
        val requirementById = criteria.flatMap { it.requirementGroups }
            .flatMap { it.requirements }
            .groupBy { it.id }

        fun Criteria.validateId() {
            val criteriaId = criteriaById.get(this.id)
            if (criteriaId != null && criteriaId.size > 1)
                throw ErrorException(
                    ErrorType.INVALID_CRITERIA,
                    message = "criteria.id is not unique"
                )
        }

        fun RequirementGroup.validateId() {
            val requirementGroup = requirementGroupById.get(this.id)
            if (requirementGroup != null && requirementGroup.size > 1)
                throw ErrorException(
                    ErrorType.INVALID_CRITERIA,
                    message = "requirementGroup.id is not unique"
                )
        }

        fun Requirement.validateId() {
            val requirement = requirementById.get(this.id)
            if (requirement != null && requirement.size > 1)
                throw ErrorException(
                    ErrorType.INVALID_CRITERIA,
                    message = "requirement.id is not unique"
                )
        }

        fun List<RequirementGroup>.validateRequirementGroupCount() {
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
    }

    private fun checkConversionRelation(criteria: List<Criteria>, conversions: List<Conversion>) {
        val requirements = criteria.flatMap { it.requirementGroups }
            .flatMap { it.requirements }
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
    }

    private fun checkCoefficient(conversions: List<Conversion>) {
        val COEFFICIENT_MIN = 0.01.toBigDecimal()
        val COEFFICIENT_MAX = 2.toBigDecimal()

        fun Coefficient.validateCoefficientRate() {
            if (this.coefficient < COEFFICIENT_MIN || this.coefficient > COEFFICIENT_MAX)
                throw ErrorException(
                    ErrorType.INVALID_CONVERSION,
                    message = "Conversion coefficient rate (${this.coefficient}) does not satisfy the conditions: " +
                        "coefficient in [${COEFFICIENT_MIN}, ${COEFFICIENT_MAX}]"
                )
        }

        conversions
            .flatMap { it.coefficients }
            .forEach { it.validateCoefficientRate() }
    }

    private fun checkCoefficientDataType(criteria: List<Criteria>, conversions: List<Conversion>) {
        fun mismatchDataTypeException(cv: CoefficientValue, rv: RequirementValue): Nothing =
            throw ErrorException(
                ErrorType.INVALID_CONVERSION,
                message = "DataType in Conversion mismatch with Requirement dataType. " +
                    "\n ${cv} != ${rv}"
            )

        fun RequirementValue.validateDataType(coefficient: Coefficient) {
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

        val requirements = criteria.flatMap { it.requirementGroups }
            .flatMap { it.requirements }
            .associateBy { it.id }

        val conversionsRelatesToRequirement = conversions.filter { it.relatesTo == ConversionsRelatesTo.REQUIREMENT }

        conversionsRelatesToRequirement.forEach {
            val requirementId = it.relatedItem
            it.coefficients.forEach { coefficient ->
                val requirement = requirements.get(requirementId)
                requirement?.value?.validateDataType(coefficient)
            }
        }
    }

    private fun checkCastCoefficient(tender: Tender) {
        fun castCoefficientException(limit: BigDecimal): Nothing =
            throw ErrorException(
                ErrorType.INVALID_CONVERSION,
                message = "cast coefficient in conversion cannot be greater than ${limit} "
            )

        val criteria: List<Criteria> = tender.criteria!!
        val conversions: List<Conversion> = tender.conversions!!.filter { it.relatesTo == ConversionsRelatesTo.REQUIREMENT }
        val items: List<Item> = tender.items

        val tenderRequirements = criteria.filter { it.relatesTo == null }
            .flatMap { it.requirementGroups }
            .flatMap { it.requirements }

        val tenderConversions = tenderRequirements.flatMap { requirement ->
            conversions.filter { it.relatedItem == requirement.id }
        }

        fun Criteria.getRelatedItems(items: List<Item>) =
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
                criteriaRelatedToItem.filter { it.relatedItem == item.id }
                    .flatMap { it.requirementGroups }
                    .flatMap { it.requirements }
            }
            val itemConversions = itemRequirement.flatMap { requirement ->
                conversions.filter { it.relatedItem == requirement.id }
            }

            val castCoefficient = (tenderConversions + lotConversions + itemConversions)
                .map { conversion -> BigDecimal(1) - conversion.coefficients.minBy { it.coefficient }!!.coefficient }
                .fold(BigDecimal.ZERO, BigDecimal::add)

            val mainProcurementCategory = tender.mainProcurementCategory

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
    }

    private fun checkConversionRelatesToEnum(conversions: List<Conversion>) {
        fun Conversion.validate() {
            when (this.relatesTo) {
                ConversionsRelatesTo.REQUIREMENT,
                ConversionsRelatesTo.OBSERVATION,
                ConversionsRelatesTo.OPTION -> Unit
            }
        }

        conversions.forEach { it.validate() }
    }

    private fun checkAwardCriteriaEnum(awardCriteria: AwardCriteria) {
        when (awardCriteria) {
            AwardCriteria.PRICE_ONLY,
            AwardCriteria.COST_ONLY,
            AwardCriteria.QUALITY_ONLY,
            AwardCriteria.RATED_CRITERIA -> Unit
        }
    }

    private fun checkAwardCriteriaDetailsEnum(awardCriteriaDetails: AwardCriteriaDetails) {
        when (awardCriteriaDetails) {
            AwardCriteriaDetails.MANUAL,
            AwardCriteriaDetails.AUTOMATED -> Unit
        }
    }

    private fun checkArrays(tender: Tender) {
        fun List<Any>.validate() {
            if (this.size < 1) throw ErrorException(
                ErrorType.INVALID_CONVERSION,
                message = "All arrays in json must have at least one object have to be added "
            )
        }

        tender.items.validate()

        tender.criteria?.validate()
        tender.criteria?.forEach { it.requirementGroups.validate() }
        tender.criteria?.forEach { it.requirementGroups.forEach { it.requirements.validate() } }

        tender.conversions?.validate()
        tender.conversions?.forEach { it.coefficients.validate() }
    }

    private fun checkAwardCriteriaDetailsAreRequired(tender: Tender) {
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
    }

    private fun checkCriteriaAndConversionAreRequired(tender: Tender) {

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
    }

    private fun convertDtoTender(tenderDto: CheckCriteriaRequest.Tender): CheckCriteriaData {
        return CheckCriteriaData(
            tender = Tender(
                awardCriteria = tenderDto.awardCriteria,
                awardCriteriaDetails = tenderDto.awardCriteriaDetails,
                mainProcurementCategory = tenderDto.mainProcurementCategory,
                criteria = tenderDto.criteria?.map { criteria ->
                    Criteria(
                        id = criteria.id,
                        description = criteria.description,
                        title = criteria.title,
                        relatesTo = criteria.relatesTo,
                        relatedItem = criteria.relatedItem,
                        requirementGroups = criteria.requirementGroups.map { rg ->
                            RequirementGroup(
                                id = rg.id,
                                requirements = rg.requirements,
                                description = rg.description
                            )
                        }
                    )
                },
                conversions = tenderDto.conversions?.map { conversion ->
                    Conversion(
                        id = conversion.id,
                        relatedItem = conversion.relatedItem,
                        rationale = conversion.rationale,
                        coefficients = conversion.coefficients.map { coefficient ->
                            Coefficient(
                                id = coefficient.id,
                                value = coefficient.value,
                                coefficient = coefficient.coefficient
                            )
                        },
                        relatesTo = conversion.relatesTo,
                        description = conversion.description
                    )
                },
                items = tenderDto.items.map { item ->
                    Item(
                        id = item.id,
                        relatedLot = item.relatedLot
                    )
                }
            )
        )
    }
}

