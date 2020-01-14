package com.procurement.dossier.infrastructure.repository

import org.testcontainers.containers.CassandraContainer

class CassandraTestContainer(tag: String = "3.11") : CassandraContainer<CassandraTestContainer>("$IMAGE:$tag") {

    val contractPoint: String
        get() = super.getContainerIpAddress()

    val port: Int
        get() = super.getMappedPort(9042)
}