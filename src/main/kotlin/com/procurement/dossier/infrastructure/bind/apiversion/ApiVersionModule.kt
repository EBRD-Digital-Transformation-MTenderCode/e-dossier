package com.procurement.dossier.infrastructure.bind.apiversion

import com.fasterxml.jackson.databind.module.SimpleModule
import com.procurement.dossier.infrastructure.dto.ApiVersion

class ApiVersionModule : SimpleModule() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 1L
    }

    init {
        addSerializer(ApiVersion::class.java, ApiVersionSerializer())
        addDeserializer(ApiVersion::class.java, ApiVersionDeserializer())
    }
}
