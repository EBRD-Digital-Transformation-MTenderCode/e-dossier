package com.procurement.dossier.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(WebConfig::class, RepositoryConfig::class, ServiceConfig::class, ObjectMapperConfiguration::class)
class ApplicationConfig