package com.procurement.dossier.infrastructure.model.dto.bpe

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.dossier.application.exception.EnumException
import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.infrastructure.bind.apiversion.ApiVersionDeserializer
import com.procurement.dossier.infrastructure.bind.apiversion.ApiVersionSerializer
import com.procurement.dossier.infrastructure.config.properties.GlobalProperties
import com.procurement.dossier.infrastructure.dto.ApiErrorResponse
import com.procurement.dossier.infrastructure.dto.ApiVersion
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import java.util.*

data class CommandMessage @JsonCreator constructor(
    @field:JsonProperty("id") @param:JsonProperty("id") val id: String,
    @field:JsonProperty("command") @param:JsonProperty("command") val command: CommandType,
    @field:JsonProperty("context") @param:JsonProperty("context") val context: Context,
    @field:JsonProperty("data") @param:JsonProperty("data") val data: JsonNode,

    @JsonDeserialize(using = ApiVersionDeserializer::class)
    @JsonSerialize(using = ApiVersionSerializer::class)
    @field:JsonProperty("version") @param:JsonProperty("version") val version: ApiVersion
)

val CommandMessage.cpid: String
    get() = this.context.cpid
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'cpid' attribute in context."
        )

val CommandMessage.token: UUID
    get() = this.context.token
        ?.let { id ->
            try {
                UUID.fromString(id)
            } catch (ignore: Exception) {
                throw ErrorException(error = ErrorType.INVALID_FORMAT_TOKEN)
            }
        }
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'token' attribute in context."
        )

val CommandMessage.owner: String
    get() = this.context.owner
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'owner' attribute in context."
        )

val CommandMessage.stage: String
    get() = this.context.stage
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'stage' attribute in context."
        )

val CommandMessage.pmd: ProcurementMethod
    get() = this.context.pmd
        ?.let {
            ProcurementMethod.valueOrException(it) {
                ErrorException(ErrorType.INVALID_PMD)
            }
        }
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'pmd' attribute in context."
        )

val CommandMessage.operationType: String
    get() = this.context.operationType
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'operationType' attribute in context."
        )

val CommandMessage.country: String
    get() = this.context.country
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'country' attribute in context."
        )

val CommandMessage.language: String
    get() = this.context.language
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'language' attribute in context."
        )

data class Context @JsonCreator constructor(
    val operationId: String?,
    val requestId: String?,
    val cpid: String?,
    val ocid: String?,
    val stage: String?,
    val prevStage: String?,
    val processType: String?,
    val operationType: String?,
    val phase: String?,
    val owner: String?,
    val country: String?,
    val language: String?,
    val pmd: String?,
    val token: String?,
    val startDate: String?,
    val endDate: String?,
    val id: String?
)

enum class CommandType(private val value: String) {

    CHECK_CRITERIA("checkCriteria"),
    CREATE_CRITERIA("createCriteria"),
    CHECK_RESPONSES("checkResponses"),
    GET_CRITERIA("getCriteria"),
    CREATE_REQUESTS_FOR_EV_PANELS("createRequestsForEvPanels");

    @JsonValue
    fun value(): String {
        return this.value
    }

    override fun toString(): String {
        return this.value
    }
}

fun errorResponse(exception: Exception, id: String, version: ApiVersion): ApiErrorResponse =
    when (exception) {
        is ErrorException -> getApiErrorResponse(
            id = id,
            version = version,
            code = exception.code,
            message = exception.message!!
        )
        is EnumException -> getApiErrorResponse(
            id = id,
            version = version,
            code = exception.code,
            message = exception.message!!
        )
        else -> getApiErrorResponse(id = id, version = version, code = "00.00", message = exception.message!!)
    }

private fun getApiErrorResponse(id: String, version: ApiVersion, code: String, message: String): ApiErrorResponse {
    return ApiErrorResponse(
        errors = listOf(
            ApiErrorResponse.Error(
                code = "400.${GlobalProperties.service.id}." + code,
                description = message
            )
        ),
        id = id,
        version = version
    )
}
