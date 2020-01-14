package com.procurement.dossier.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cassandra")
data class CassandraProperties(

    var contactPoints: String?,

    var keyspaceName: String?,

    var username: String?,

    var password: String?
) {
    fun getContactPoints(): Array<String> {
        return this.contactPoints!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }
}