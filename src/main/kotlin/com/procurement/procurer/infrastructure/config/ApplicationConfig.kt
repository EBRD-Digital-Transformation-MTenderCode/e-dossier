package com.procurement.procurer.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(WebConfig::class, ServiceConfig::class, ObjectMapperConfig::class)
class ApplicationConfig