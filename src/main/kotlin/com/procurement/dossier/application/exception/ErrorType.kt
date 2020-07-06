package com.procurement.dossier.application.exception

enum class ErrorType constructor(val code: String, val message: String) {
    INVALID_JSON_TYPE("00.00", "Invalid type: "),
    INVALID_PMD("10.13", "Invalid pmd."),
    CONTEXT("20.01", "Context parameter not found."),
    INVALID_FORMAT_TOKEN("10.63", "Invalid format the award id."),
    INVALID_REQUIREMENT_VALUE("10.66", "Invalid requirement value."),
    INVALID_PERIOD_VALUE("10.67", "Invalid period value."),
    INVALID_CRITERIA("10.68", "Invalid criteria value."),
    INVALID_CONVERSION("10.69", "Invalid conversion value."),
    INVALID_AWARD_CRITERIA("10.70", "Invalid award criteria."),
    INVALID_JSON("10.71", "Invalid json."),
    ENTITY_NOT_FOUND("10.72", "No entity found in db with specified parameters."),
    INVALID_REQUIREMENT_RESPONSE("10.73", "Invalid requirement response."),
    EMPTY_LIST("10.74", "Collection is empty."),
    NOT_UNIQUE_IDS("10.75", "Elements in collection is not unique by id."),
    INVALID_COEFFICIENT("10.76", "Invalid coefficient."),
    INVALID_PERIOD_DATES("10.77", "Period start date must precede end date."),
    PERIOD_RULE_NOT_FOUND("10.78", "Period rule not found."),
    INVALID_PERIOD_DURATION("10.79", "Period duration is invalid"),
    INVALID_PERIOD_END_DATE("10.80", "Period end date must be equal or greater than previously stored period end date."),
    PERIOD_NOT_FOUND("10.81", "Period not found."),
}
