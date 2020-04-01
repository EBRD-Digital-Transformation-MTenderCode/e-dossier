package com.procurement.dossier.infrastructure.config


import com.procurement.dossier.infrastructure.service.logger.CustomLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoggerConfiguration {

    @Bean
    fun logger() = CustomLogger()
}
