package com.procurement.dossier.infrastructure.service

import com.procurement.dossier.application.service.Generable
import com.procurement.dossier.domain.model.Token
import java.util.*

class GenerationService : Generable {

    override fun generatePermanentCriteriaId(): UUID {
        return UUID.randomUUID()
    }

    override fun generatePermanentRequirementGroupId(): UUID {
        return UUID.randomUUID()
    }

    override fun generatePermanentRequirementId(): UUID {
        return UUID.randomUUID()
    }

    override fun generatePermanentConversionId(): UUID {
        return UUID.randomUUID()
    }

    override fun generatePermanentCoefficientId(): UUID {
        return UUID.randomUUID()
    }

    override fun generateToken(): Token {
        return UUID.randomUUID()
    }
}