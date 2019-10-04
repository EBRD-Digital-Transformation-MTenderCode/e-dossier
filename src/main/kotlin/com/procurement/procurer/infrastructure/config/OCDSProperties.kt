package com.procurement.procurer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ocds")
class OCDSProperties {
    var prefix: String? = null
}

