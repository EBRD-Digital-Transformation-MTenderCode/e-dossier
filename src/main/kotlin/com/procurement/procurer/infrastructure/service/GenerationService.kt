package com.procurement.procurer.infrastructure.service

import com.procurement.procurer.application.service.Generable
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
}