package com.procurement.procurer.application.service

import java.util.*

interface Generable {

    fun generatePermanentCriteriaId(): UUID

    fun generatePermanentRequirementGroupId(): UUID

    fun generatePermanentRequirementId(): UUID

    fun generatePermanentConversionId(): UUID

    fun generatePermanentCoefficientId(): UUID
}