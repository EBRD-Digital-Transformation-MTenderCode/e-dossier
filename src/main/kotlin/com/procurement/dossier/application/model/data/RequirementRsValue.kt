package com.procurement.dossier.application.model.data

import com.procurement.dossier.infrastructure.model.dto.ocds.RequirementDataType
import java.math.BigDecimal

sealed class RequirementRsValue {

    val dataType: RequirementDataType
        get() = when (this) {
            is AsBoolean -> RequirementDataType.BOOLEAN
            is AsString -> RequirementDataType.STRING
            is AsNumber -> RequirementDataType.NUMBER
            is AsInteger -> RequirementDataType.INTEGER
        }

    data class AsBoolean(val value: Boolean) : RequirementRsValue()
    data class AsString(val value: String) : RequirementRsValue()
    data class AsNumber(val value: BigDecimal) : RequirementRsValue()
    data class AsInteger(val value: Long) : RequirementRsValue()
}