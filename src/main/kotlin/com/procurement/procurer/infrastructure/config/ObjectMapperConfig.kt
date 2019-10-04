package com.procurement.procurer.infrastructure.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.procurement.procurer.infrastructure.bind.databinding.IntDeserializer
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeDeserializer
import com.procurement.procurer.infrastructure.bind.databinding.JsonDateTimeSerializer
import com.procurement.procurer.infrastructure.bind.databinding.StringsDeserializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime

@Configuration
class ObjectMapperConfig(@Autowired objectMapper: ObjectMapper) {

    init {
        val module = SimpleModule()
        module.addSerializer(LocalDateTime::class.java,
                             JsonDateTimeSerializer()
        )
        module.addDeserializer(LocalDateTime::class.java,
                               JsonDateTimeDeserializer()
        )
        module.addDeserializer(String::class.java,
                               StringsDeserializer()
        )
        module.addDeserializer(Int::class.java,
                               IntDeserializer()
        )
        objectMapper.registerModule(module)
        objectMapper.registerModule(KotlinModule())
        objectMapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
    }
}
