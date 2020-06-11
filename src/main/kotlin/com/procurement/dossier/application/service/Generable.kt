package com.procurement.dossier.application.service

import com.procurement.dossier.domain.model.Token
import java.util.*

interface Generable {

    fun generatePermanentCriteriaId(): UUID

    fun generatePermanentRequirementGroupId(): UUID

    fun generatePermanentRequirementId(): UUID

    fun generatePermanentConversionId(): UUID

    fun generatePermanentCoefficientId(): UUID

    fun generateToken(): Token
}