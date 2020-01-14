package com.procurement.dossier.infrastructure.exception

class CoefficientException(coefficient: String, description: String = "") :
    RuntimeException("Incorrect coefficient: '$coefficient'. $description")
