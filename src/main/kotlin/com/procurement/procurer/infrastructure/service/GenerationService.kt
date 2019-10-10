package com.procurement.procurer.infrastructure.service

import com.procurement.procurer.infrastructure.config.OCDSProperties
import com.procurement.procurer.infrastructure.utils.milliNowUTC
import org.springframework.stereotype.Service
import java.util.*

@Service
class GenerationService(private val ocdsProperties: OCDSProperties) {

    fun getCpId(country: String): String {
        return ocdsProperties.prefix + "-" + country + "-" + milliNowUTC()
    }

    fun generatePermanentCriteriaId(): String {
        return UUID.randomUUID().toString()
    }

    fun generatePermanentRequirementGroupId(): String {
        return UUID.randomUUID().toString()
    }

    fun generatePermanentRequirementId(): String {
        return UUID.randomUUID().toString()
    }

    fun generatePermanentConversionId(): String {
        return UUID.randomUUID().toString()
    }

    fun generatePermanentCoefficientId(): String {
        return UUID.randomUUID().toString()
    }
}