package com.procurement.dossier.infrastructure.exception

class EnumElementProviderException(enumType: String, value: String, values: String) : RuntimeException(
    "Unknown value for enumType $enumType: $value, Allowed values are $values"
)
