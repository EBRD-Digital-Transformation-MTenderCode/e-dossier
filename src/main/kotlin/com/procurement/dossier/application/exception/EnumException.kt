package com.procurement.dossier.application.exception

data class EnumException(private val enumType: String, val value: String, val values: String) : RuntimeException(
    "Unknown value for enumType $enumType: $value, Allowed values are $values"
) {
    val code: String = "00.00"
}
