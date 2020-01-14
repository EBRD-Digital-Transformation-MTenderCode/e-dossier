package com.procurement.dossier

import com.procurement.dossier.infrastructure.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackageClasses = [ApplicationConfig::class])
class ProcurerApplication

fun main(args: Array<String>) {
    runApplication<ProcurerApplication>(*args)
}

