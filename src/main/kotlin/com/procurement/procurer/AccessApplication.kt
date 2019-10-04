package com.procurement.procurer

import com.procurement.procurer.infrastructure.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackageClasses = [ApplicationConfig::class])
class AccessApplication

fun main(args: Array<String>) {
    runApplication<AccessApplication>(*args)
}

