package com.procurement.procurer.application.service

interface Generable {

    fun generatePermanentCriteriaId(): String

    fun generatePermanentRequirementGroupId(): String

    fun generatePermanentRequirementId(): String

    fun generatePermanentConversionId(): String

    fun generatePermanentCoefficientId(): String
}