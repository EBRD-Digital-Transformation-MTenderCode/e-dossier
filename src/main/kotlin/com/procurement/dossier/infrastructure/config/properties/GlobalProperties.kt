package com.procurement.dossier.infrastructure.config.properties

import com.procurement.dossier.infrastructure.dto.ApiVersion

object GlobalProperties {
    const val serviceId = "19"

    object App {
        val apiVersion = ApiVersion(major = 1, minor = 0, patch = 0)
    }
}
