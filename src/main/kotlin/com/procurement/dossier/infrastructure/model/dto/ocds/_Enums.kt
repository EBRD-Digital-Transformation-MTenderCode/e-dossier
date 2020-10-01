package com.procurement.dossier.infrastructure.model.dto.ocds

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class AwardCriteria(@JsonValue override val key: String) : EnumElementProvider.Key {
    PRICE_ONLY("priceOnly"),
    COST_ONLY("costOnly"),
    QUALITY_ONLY("qualityOnly"),
    RATED_CRITERIA("ratedCriteria");

    override fun toString(): String = key

    companion object : EnumElementProvider<AwardCriteria>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = AwardCriteria.orThrow(name)
    }
}

enum class AwardCriteriaDetails(@JsonValue override val key: String) : EnumElementProvider.Key {
    MANUAL("manual"),
    AUTOMATED("automated");

    override fun toString(): String = key

    companion object : EnumElementProvider<AwardCriteriaDetails>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = AwardCriteriaDetails.orThrow(name)
    }
}

enum class CriteriaSource(@JsonValue override val key: String) : EnumElementProvider.Key {
    TENDERER("tenderer"),
    BUYER("buyer"),
    PROCURING_ENTITY("procuringEntity");

    override fun toString(): String = key

    companion object : EnumElementProvider<CriteriaSource>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = CriteriaSource.orThrow(name)
    }
}

enum class CriteriaRelatesTo(@JsonValue override val key: String) : EnumElementProvider.Key {
    TENDERER("tenderer"),
    ITEM("item"),
    LOT("lot"),
    AWARD ("award");

    override fun toString(): String = key

    companion object : EnumElementProvider<CriteriaRelatesTo>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = CriteriaRelatesTo.orThrow(name)
    }
}

enum class ConversionsRelatesTo(@JsonValue override val key: String) : EnumElementProvider.Key {
    REQUIREMENT("requirement");

    override fun toString(): String = key

    companion object : EnumElementProvider<ConversionsRelatesTo>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = ConversionsRelatesTo.orThrow(name)
    }
}

enum class RequirementDataType(@JsonValue override val key: String) : EnumElementProvider.Key {

    NUMBER("number"),
    BOOLEAN("boolean"),
    STRING("string"),
    INTEGER("integer");

    override fun toString(): String = key

    companion object : EnumElementProvider<RequirementDataType>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = RequirementDataType.orThrow(name)
    }
}

enum class MainProcurementCategory(@JsonValue override val key: String) : EnumElementProvider.Key {
    GOODS("goods"),
    WORKS("works"),
    SERVICES("services");

    override fun toString(): String = key

    companion object : EnumElementProvider<MainProcurementCategory>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = MainProcurementCategory.orThrow(name)
    }
}

enum class ProcurementMethod(@JsonValue val value: String) {
    CD("selective"),
    CF("selective"),
    DA("limited"),
    DC("selective"),
    FA("limited"),
    GPA("selective"),
    IP("selective"),
    MV("open"),
    NP("limited"),
    OF("selective"),
    OP("selective"),
    OT("open"),
    RT("selective"),
    SV("open"),
    TEST_CD("selective"),
    TEST_CF("selective"),
    TEST_DA("limited"),
    TEST_DC("selective"),
    TEST_FA("limited"),
    TEST_GPA("selective"),
    TEST_IP("selective"),
    TEST_MV("open"),
    TEST_NP("limited"),
    TEST_OF("selective"),
    TEST_OP("selective"),
    TEST_OT("open"),
    TEST_RT("selective"),
    TEST_SV("open");

    override fun toString(): String {
        return this.value
    }

    companion object {
        fun <T : Exception> valueOrException(name: String, block: (Exception) -> T): ProcurementMethod = try {
            valueOf(name)
        } catch (exception: Exception) {
            throw block(exception)
        }

        val allowedElements: List<ProcurementMethod> = enumValues<ProcurementMethod>().toList()
    }
}

enum class Operation(@JsonValue override val key: String) : EnumElementProvider.Key {
    COMPLETE_QUALIFICATION("completeQualification"),
    CREATE_CN("createCN"),
    CREATE_PCR("createPcr"),
    CREATE_PN("createPN"),
    CREATE_PIN("createPIN"),
    CREATE_CN_ON_PN("createCNonPN"),
    CREATE_CN_ON_PIN("createCNonPIN"),
    CREATE_PIN_ON_PN("createPINonPN"),
    CREATE_NEGOTIATION_CN_ON_PN("createNegotiationCnOnPn"),
    QUALIFICATION("qualification"),
    START_SECOND_STAGE("startSecondStage"),
    SUBMISSION_PERIOD_END("submissionPeriodEnd"),
    UPDATE_CN("updateCN"),
    UPDATE_PN("updatePN");

    override fun toString(): String = key

    companion object : EnumElementProvider<Operation>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = Operation.orThrow(name)
    }
}