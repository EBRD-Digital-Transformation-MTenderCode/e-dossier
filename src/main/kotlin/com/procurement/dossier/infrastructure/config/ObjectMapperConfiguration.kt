package com.procurement.dossier.infrastructure.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.procurement.dossier.infrastructure.bind.databinding.IntDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.dossier.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.dossier.infrastructure.bind.databinding.StringsDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime

@Configuration
class ObjectMapperConfiguration() {
    @Bean
    fun mapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        val module = SimpleModule()
        module.addSerializer(
            LocalDateTime::class.java,
            JsonDateTimeSerializer()
        )
        module.addDeserializer(
            LocalDateTime::class.java,
            JsonDateTimeDeserializer()
        )
        module.addDeserializer(
            String::class.java,
            StringsDeserializer()
        )
        module.addDeserializer(
            Int::class.java,
            IntDeserializer()
        )
        objectMapper.registerModule(module)
        objectMapper.registerModule(KotlinModule())
        objectMapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.nodeFactory = JsonNodeFactory.withExactBigDecimals(true)

        return objectMapper
    }
}
