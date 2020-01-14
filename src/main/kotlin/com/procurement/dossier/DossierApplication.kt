package com.procurement.dossier

import com.procurement.dossier.infrastructure.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackageClasses = [ApplicationConfig::class])
class DossierApplication

fun main(args: Array<String>) {
    runApplication<DossierApplication>(*args)
}

