package com.procurement.dossier.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.enums.ResponseStatus
import java.time.LocalDateTime
import java.util.*

@JsonPropertyOrder("version", "id", "status", "result")
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

    class Error(
        val code: String,
        val description: String,

        @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
        val details: List<Detail>? = null
    ) {
        class Detail(
            @field:JsonInclude(JsonInclude.Include.NON_NULL)
            val name: String? = null,

            @field:JsonInclude(JsonInclude.Include.NON_NULL)
            val id: String? = null
        )
    }
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
        class Detail(
            val code: String,
            val description: String,

            @field:JsonInclude(JsonInclude.Include.NON_NULL)
            val metadata: Any?
        )
    }
}
