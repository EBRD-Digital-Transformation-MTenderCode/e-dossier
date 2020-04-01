package com.procurement.dossier.infrastructure.config.properties

import com.procurement.dossier.infrastructure.dto.ApiVersion
import com.procurement.dossier.infrastructure.io.orThrow
import java.util.*

object GlobalProperties {

    val service = GlobalProperties.Service()

    object App {
        val apiVersion = ApiVersion(major = 1, minor = 0, patch = 0)
    }

    class Service(
        val id: String = "19",
        val name: String = "e-dossier",
        val version: String = getGitProperties()
    )

    private fun getGitProperties(): String {
        val gitProps: Properties = try {
            GlobalProperties::class.java.getResourceAsStream("/git.properties")
                .use { stream ->
                    Properties().apply { load(stream) }
                }
        } catch (expected: Exception) {
            throw IllegalStateException(expected)
        }
        return gitProps.orThrow("git.commit.id.abbrev")
    }
}
