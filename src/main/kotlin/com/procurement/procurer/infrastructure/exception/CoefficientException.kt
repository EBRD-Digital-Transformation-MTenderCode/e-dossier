package com.procurement.procurer.infrastructure.exception

class CoefficientException(coefficient: String, description: String = "") :
    RuntimeException("Incorrect coefficient: '$coefficient'. $description")
