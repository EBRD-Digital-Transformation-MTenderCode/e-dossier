package com.procurement.dossier.infrastructure.model.dto.ocds

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.application.exception.EnumException
import java.util.*

enum class AwardCriteria(@JsonValue val value: String) {
    PRICE_ONLY("priceOnly"),
    COST_ONLY("costOnly"),
    QUALITY_ONLY("qualityOnly"),
    RATED_CRITERIA("ratedCriteria");

    override fun toString(): String {
        return this.value
    }

    companion object {
        private val elements: Map<String, AwardCriteria> =
            values().associateBy { it.value.toUpperCase() }

        fun fromString(value: String): AwardCriteria =
            elements[value.toUpperCase()] ?: throw EnumException(
                enumType = AwardCriteria::class.java.canonicalName,
                value = value,
                values = values().joinToString { it.value })
    }
}

enum class AwardCriteriaDetails(@JsonValue val value: String) {
    MANUAL("manual"),
    AUTOMATED("automated");

    override fun toString(): String {
        return this.value
    }

    companion object {
        private val elements: Map<String, AwardCriteriaDetails> =
            values().associateBy { it.value.toUpperCase() }

        fun fromString(value: String): AwardCriteriaDetails =
            elements[value.toUpperCase()] ?: throw EnumException(
                enumType = AwardCriteriaDetails::class.java.canonicalName,
                value = value,
                values = values().joinToString { it.value })
    }
}

enum class CriteriaSource(@JsonValue val value: String) {
    TENDERER("tenderer"),
    BUYER("buyer"),
    PROCURING_ENTITY("procuringEntity");

    override fun toString(): String {
        return this.value
    }

    companion object {
        private val elements: Map<String, CriteriaSource> =
            values().associateBy { it.value.toUpperCase() }

        fun fromString(value: String): CriteriaSource =
            elements[value.toUpperCase()] ?: throw EnumException(
                enumType = CriteriaSource::class.java.canonicalName,
                value = value,
                values = values().joinToString { it.value })
    }
}

enum class CriteriaRelatesTo(@JsonValue val value: String) {
    TENDERER("tenderer"),
    ITEM("item"),
    LOT("lot"),
    AWARD ("award");

    override fun toString(): String {
        return this.value
    }
}

enum class ConversionsRelatesTo(@JsonValue val value: String) {
    REQUIREMENT("requirement");

    override fun toString(): String {
        return this.value
    }
}

enum class RequirementDataType(private val value: String) {

    NUMBER("number"),
    BOOLEAN("boolean"),
    STRING("string"),
    INTEGER("integer");

    @JsonValue
    fun value(): String {
        return this.value
    }

    override fun toString(): String {
        return this.value
    }

    companion object {
        private val elements: Map<String, RequirementDataType> =
            values().associateBy { it.value.toUpperCase() }

        fun fromString(value: String): RequirementDataType =
            elements[value.toUpperCase()] ?: throw EnumException(
                enumType = RequirementDataType::class.java.canonicalName,
                value = value,
                values = values().joinToString { it.value })
    }
}

enum class MainProcurementCategory(@JsonValue val value: String) {
    GOODS("goods"),
    WORKS("works"),
    SERVICES("services");

    override fun toString(): String {
        return this.value
    }
}

enum class ProcurementMethod(@JsonValue val value: String) {
    MV("open"),
    OT("open"),
    RT("selective"),
    SV("open"),
    DA("limited"),
    NP("limited"),
    FA("limited"),
    OP("selective"),
    GPA("selective"),
    TEST_OT("open"),
    TEST_SV("open"),
    TEST_RT("selective"),
    TEST_MV("open"),
    TEST_DA("limited"),
    TEST_NP("limited"),
    TEST_FA("limited"),
    TEST_OP("selective"),
    TEST_GPA("selective"),;

    override fun toString(): String {
        return this.value
    }

    companion object {
        fun <T : Exception> valueOrException(name: String, block: (Exception) -> T): ProcurementMethod = try {
            valueOf(name)
        } catch (exception: Exception) {
            throw block(exception)
        }
    }
}

enum class Operation(val value: String) {
    CREATE_CN("createCN"),
    CREATE_PN("createPN"),
    CREATE_PIN("createPIN"),
    UPDATE_CN("updateCN"),
    UPDATE_PN("updatePN"),
    CREATE_CN_ON_PN("createCNonPN"),
    CREATE_CN_ON_PIN("createCNonPIN"),
    CREATE_PIN_ON_PN("createPINonPN"),
    CREATE_NEGOTIATION_CN_ON_PN("createNegotiationCnOnPn");

    companion object {
        private val CONSTANTS = HashMap<String, Operation>()

        init {
            for (c in Operation.values()) {
                CONSTANTS[c.value] = c
            }
        }

        @JsonCreator
        fun fromValue(value: String): Operation {
            return CONSTANTS[value] ?: throw IllegalArgumentException(value)
        }
    }
}