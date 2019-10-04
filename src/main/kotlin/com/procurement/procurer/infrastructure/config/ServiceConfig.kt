package com.procurement.procurer.infrastructure.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(
    basePackages = [
        "com.procurement.procurer.infrastructure.service"
    ]
)
class ServiceConfig
