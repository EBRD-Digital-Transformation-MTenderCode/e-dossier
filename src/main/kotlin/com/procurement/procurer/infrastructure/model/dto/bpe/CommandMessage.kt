package com.procurement.procurer.infrastructure.model.dto.bpe

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.procurement.procurer.application.exception.EnumException
import com.procurement.procurer.application.exception.ErrorException
import com.procurement.procurer.application.exception.ErrorType
import com.procurement.procurer.infrastructure.model.dto.ocds.ProcurementMethod
import java.util.*

data class CommandMessage @JsonCreator constructor(

    val id: String,
    val command: CommandType,
    val context: Context,
    val data: JsonNode,
    val version: ApiVersion
)

val CommandMessage.cpid: String
    get() = this.context.cpid
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'cpid' attribute in context."
        )

val CommandMessage.token: UUID
    get() = this.context.token?.let { id ->
        try {
            UUID.fromString(id)
        } catch (exception: Exception) {
            throw ErrorException(error = ErrorType.INVALID_FORMAT_TOKEN)
        }
    } ?: throw ErrorException(
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
    get() = this.context.pmd?.let {
        ProcurementMethod.valueOrException(it) {
            ErrorException(ErrorType.INVALID_PMD)
        }
    } ?: throw ErrorException(
        error = ErrorType.CONTEXT,
        message = "Missing the 'pmd' attribute in context."
    )

val CommandMessage.operationType: String
    get() = this.context.operationType
        ?: throw ErrorException(
            error = ErrorType.CONTEXT,
            message = "Missing the 'operationType' attribute in context."
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
    CREATE_CRITERIA("createCriteria");

    @JsonValue
    fun value(): String {
        return this.value
    }

    override fun toString(): String {
        return this.value
    }
}

enum class ApiVersion(private val value: String) {
    V_0_0_1("0.0.1");

    @JsonValue
    fun value(): String {
        return this.value
    }

    override fun toString(): String {
        return this.value
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResponseDto(

    val errors: List<ResponseErrorDto>? = null,

    val data: Any? = null,

    val id: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResponseErrorDto(

    val code: String,

    val description: String?
)

fun getExceptionResponseDto(exception: Exception): ResponseDto {
    return ResponseDto(
        errors = listOf(
            ResponseErrorDto(
                code = "400.19.00",
                description = exception.message ?: exception.toString()
            )
        )
    )
}

fun getErrorExceptionResponseDto(exception: ErrorException, id: String? = null): ResponseDto {
    return ResponseDto(
        errors = listOf(
            ResponseErrorDto(
                code = "400.19." + exception.error.code,
                description = exception.message
            )
        ),
        id = id
    )
}

fun getEnumExceptionResponseDto(error: EnumException, id: String? = null): ResponseDto {
    return ResponseDto(
        errors = listOf(
            ResponseErrorDto(
                code = "400.19." + error.code,
                description = error.msg
            )
        ),
        id = id
    )
}

