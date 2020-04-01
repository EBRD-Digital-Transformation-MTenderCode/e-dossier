package com.procurement.dossier.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.procurement.dossier.infrastructure.bind.jackson.configuration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectMapperConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().apply { configuration() }

}
