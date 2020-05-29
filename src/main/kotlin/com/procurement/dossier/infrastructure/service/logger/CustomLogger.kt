package com.procurement.dossier.infrastructure.service.logger

import com.procurement.dossier.application.service.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class CustomLogger : Logger {
    companion object {
        private val log: org.slf4j.Logger = LoggerFactory.getLogger(CustomLogger::class.java)
    }

    override val isDebugEnabled: Boolean
        get() = log.isDebugEnabled

    override fun error(message: String, mdc: Map<String, String>, exception: Exception?) {
        MDC.setContextMap(mdc)
        log.error(message, exception)
        MDC.clear()
    }

    override fun warn(message: String, mdc: Map<String, String>, exception: Exception?) {
        MDC.setContextMap(mdc)
        log.warn(message, exception)
        MDC.clear()
    }

    override fun info(message: String, mdc: Map<String, String>, exception: Exception?) {
        MDC.setContextMap(mdc)
        log.info(message, exception)
        MDC.clear()
    }

    override fun debug(message: String) {
        log.debug(message)
    }
}
