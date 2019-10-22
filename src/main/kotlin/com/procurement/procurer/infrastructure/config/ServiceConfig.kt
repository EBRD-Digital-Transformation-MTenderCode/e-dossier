package com.procurement.procurer.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.procurement.procurer.infrastructure.service.MedeiaValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(
    basePackages = [
        "com.procurement.procurer.infrastructure.service"
    ]
)
class ServiceConfig {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Bean
    fun jsonValidator(): MedeiaValidationService {
        return MedeiaValidationService(objectMapper)
    }

}
