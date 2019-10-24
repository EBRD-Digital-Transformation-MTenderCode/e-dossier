package com.procurement.procurer.infrastructure.exception

class RequirementValueException(requirementValue: String, description: String = "") :
    RuntimeException("Incorrect value in requirement: '$requirementValue'. $description")
