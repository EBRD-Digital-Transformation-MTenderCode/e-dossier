package com.procurement.procurer.infrastructure.service

import com.procurement.procurer.application.service.Generable
import org.springframework.stereotype.Service
import java.util.*

@Service
class GenerationService : Generable {

    override fun generatePermanentCriteriaId(): String {
        return UUID.randomUUID().toString()
    }

    override fun generatePermanentRequirementGroupId(): String {
        return UUID.randomUUID().toString()
    }

    override fun generatePermanentRequirementId(): String {
        return UUID.randomUUID().toString()
    }

    override fun generatePermanentConversionId(): String {
        return UUID.randomUUID().toString()
    }

    override fun generatePermanentCoefficientId(): String {
        return UUID.randomUUID().toString()
    }
}