package com.procurement.dossier.domain.fail.error

import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.application.service.Logger
sealed class DataErrors(numberError: String, override val description: String) : Fail.Error("DR-") {

    override val code: String = prefix + numberError

    sealed class Validation(numberError: String, val name: String, description: String) :
        DataErrors(numberError = numberError, description = description) {

        companion object {
            const val ATTRIBUTE_NAME_KEY = "attributeName"
        }

        override fun logging(logger: Logger) {
            logger.error(message = message, mdc = mapOf(ATTRIBUTE_NAME_KEY to name))
        }

        class MissingRequiredAttribute(name: String) :
            Validation(numberError = "1", description = "Missing required attribute.", name = name)

        class DataTypeMismatch(name: String, expectedType: String, actualType: String) :
            Validation(
                numberError = "2",
                description = "Data type mismatch. Expected data type: '$expectedType', actual data type: '$actualType'.",
                name = name
            )

        class UnknownValue(name: String, expectedValues: Collection<String>, actualValue: String) :
            Validation(
                numberError = "3",
                description = "Attribute value mismatch with one of enum expected values. Expected values: '${expectedValues.joinToString()}', actual value: '$actualValue'.",
                name = name
            )

        class DataFormatMismatch(name: String, expectedFormat: String, actualValue: String) :
            Validation(
                numberError = "4",
                description = "Data format mismatch. Expected data format: '$expectedFormat', actual value: '$actualValue'.",
                name = name
            )

        class DataMismatchToPattern(name: String, pattern: String, actualValue: String) :
            Validation(
                numberError = "5",
                description = "Data mismatch to pattern: '$pattern'. Actual value: '$actualValue'.",
                name = name
            )

        class UniquenessDataMismatch(name: String, value: String) :
            Validation(numberError = "6", description = "Uniqueness data mismatch: '$value'.", name = name)

        class InvalidNumberOfElementsInArray(name: String, min: Int? = null, max: Int? = null, actualLength: Int) :
            Validation(
                numberError = "7",
                description = "Invalid number of objects in the array. Expected length from '${min ?: "none min"}' to '${max ?: "none max"}', actual length: '$actualLength'.",
                name = name
            )

        class InvalidStringLength(name: String, min: Int? = null, max: Int? = null, actualLength: Int) :
            Validation(
                numberError = "8",
                description = "Invalid number of chars in string. Expected length from '${min ?: "none min"}' to '${max ?: "none max"}', actual length: '$actualLength'.",
                name = name
            )

        class EmptyObject(name: String) :
            Validation(numberError = "9", description = "Object is empty.", name = name)

        class EmptyArray(name: String) :
            Validation(numberError = "10", description = "Array is empty.", name = name)

        class EmptyString(name: String) :
            Validation(numberError = "11", description = "String is empty.", name = name)

        class UnexpectedAttribute(name: String) :
            Validation(numberError = "12", description = "Unexpected attribute.", name = name)
    }
}
