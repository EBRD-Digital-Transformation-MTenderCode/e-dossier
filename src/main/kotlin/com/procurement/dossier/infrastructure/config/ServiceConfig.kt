package com.procurement.dossier.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.dossier.application.repository.CriteriaRepository
import com.procurement.dossier.application.repository.PeriodRepository
import com.procurement.dossier.application.repository.PeriodRulesRepository
import com.procurement.dossier.application.service.CriteriaService
import com.procurement.dossier.application.service.PeriodService
import com.procurement.dossier.infrastructure.service.GenerationService
import com.procurement.dossier.infrastructure.service.MedeiaValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(
    basePackages = [
        "com.procurement.dossier.infrastructure.service",
        "com.procurement.dossier.infrastructure.handler",
        "com.procurement.dossier.application.service"
    ]
)
class ServiceConfig {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var criteriaRepository: CriteriaRepository

    @Autowired
    private lateinit var periodRepository: PeriodRepository

    @Autowired
    private lateinit var periodRulesRepository: PeriodRulesRepository

    @Bean
    fun jsonValidator(): MedeiaValidationService {
        return MedeiaValidationService(objectMapper)
    }

    @Bean
    fun generationService(): GenerationService {
        return GenerationService()
    }

    @Bean
    fun criteriaService(): CriteriaService {
        return CriteriaService(
            generationService(),
            criteriaRepository,
            jsonValidator()
        )
    }

    @Bean
    fun periodService(): PeriodService = PeriodService(periodRulesRepository, periodRepository)

}
