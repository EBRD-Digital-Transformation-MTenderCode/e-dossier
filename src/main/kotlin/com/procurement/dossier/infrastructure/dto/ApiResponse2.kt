package com.procurement.dossier.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.enums.ResponseStatus
import java.time.LocalDateTime
import java.util.*

sealed class ApiResponse2(
    @field:JsonProperty("id") @param:JsonProperty("id") val id: UUID,

    @field:JsonProperty("version") @param:JsonProperty("version") val version: ApiVersion,

    @field:JsonProperty("result") @param:JsonProperty("result") val result: Any?
) {
    abstract val status: ResponseStatus
}

class ApiSuccessResponse2(
    version: ApiVersion,
    id: UUID,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) result: Any? = null
) : ApiResponse2(
    version = version,
    result = result,
    id = id
) {
    @field:JsonProperty("status")
    override val status: ResponseStatus = ResponseStatus.SUCCESS
}

class ApiErrorResponse2(
    version: ApiVersion,
    id: UUID,
    result: List<Error>
) : ApiResponse2(
    version = version,
    result = result,
    id = id
) {
    @field:JsonProperty("status")
    override val status: ResponseStatus = ResponseStatus.ERROR

    class Error(val code: String?, val description: String?)
}

class ApiDataErrorResponse2(
    version: ApiVersion,
    id: UUID,
    result: List<Error>
) : ApiResponse2(
    version = version,
    result = result,
    id = id
) {
    @field:JsonProperty("status")
    override val status: ResponseStatus = ResponseStatus.ERROR

    class Error(val code: String?, val description: String?, val details: List<Detail>)
    class Detail(val name: String)
}

class ApiIncidentResponse2(
    version: ApiVersion,
    id: UUID,
    result: Incident
) : ApiResponse2(
    version = version,
    result = result,
    id = id
) {
    @field:JsonProperty("status")
    override val status: ResponseStatus = ResponseStatus.INCIDENT

    class Incident(
        val id: UUID,
        val date: LocalDateTime,
        val level: Fail.Incident.Level,
        val service: Service,
        val details: List<Detail>
    ) {
        class Service(val id: String, val name: String, val version: String)
        class Detail(val code: String, val description: String, val metadata: Any?)
    }
}
