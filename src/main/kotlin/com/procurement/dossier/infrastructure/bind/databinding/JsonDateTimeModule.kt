package com.procurement.dossier.infrastructure.bind.databinding

import com.fasterxml.jackson.databind.module.SimpleModule
import java.time.LocalDateTime

class JsonDateTimeModule : SimpleModule() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 1L
    }

    init {
        addSerializer(LocalDateTime::class.java, JsonDateTimeSerializer())
        addDeserializer(LocalDateTime::class.java, JsonDateTimeDeserializer())
    }
}
