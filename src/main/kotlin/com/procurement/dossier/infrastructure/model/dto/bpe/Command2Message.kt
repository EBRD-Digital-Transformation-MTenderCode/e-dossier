package com.procurement.dossier.infrastructure.model.dto.bpe

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.NullNode
import com.procurement.dossier.domain.EnumElementProvider
import com.procurement.dossier.domain.EnumElementProvider.Companion.keysAsStrings
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.BadRequestErrors
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.util.Action
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.Result.Companion.success
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.bind
import com.procurement.dossier.infrastructure.config.properties.GlobalProperties
import com.procurement.dossier.infrastructure.dto.ApiErrorResponse2
import com.procurement.dossier.infrastructure.dto.ApiIncidentResponse2
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.dto.ApiVersion
import com.procurement.dossier.infrastructure.utils.toList
import com.procurement.dossier.infrastructure.utils.tryToObject
import java.time.LocalDateTime
import java.util.*

enum class Command2Type(@JsonValue override val key: String) : Action, EnumElementProvider.Key {
    CHECK_ACCESS_TO_SUBMISSION("checkAccessToSubmission"),
    CHECK_PERIOD_2("checkPeriod"),
    CHECK_PRESENCE_CANDIDATE_IN_ONE_SUBMISSION("checkPresenceCandidateInOneSubmission"),
    CREATE_SUBMISSION("createSubmission"),
    FINALIZE_SUBMISSIONS("finalizeSubmissions"),
    FIND_SUBMISSIONS("findSubmissions"),
    GET_ORGANIZATIONS("getOrganizations"),
    GET_SUBMISSION_PERIOD_END_DATE("getSubmissionPeriodEndDate"),
    GET_SUBMISSION_STATE_BY_IDS("getSubmissionStateByIds"),
    GET_SUBMISSIONS_BY_QUALIFICATION_IDS("getSubmissionsByQualificationIds"),
    SET_STATE_FOR_SUBMISSION("setStateForSubmission"),
    VALIDATE_REQUIREMENT_RESPONSE("validateRequirementResponse"),
    VALIDATE_SUBMISSION("validateSubmission"),
    VERIFY_SUBMISSION_PERIOD_END("verifySubmissionPeriodEnd");

    override fun toString(): String = key

    companion object : EnumElementProvider<Command2Type>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = Command2Type.orThrow(name)
    }
}

fun errorResponse(fail: Fail, id: UUID = NaN, version: ApiVersion = GlobalProperties.App.apiVersion): ApiResponse2 =
    when (fail) {
        is DataErrors.Validation -> generateDataErrorResponse(id = id, version = version, fail = fail)
        is Fail.Error -> generateErrorResponse(id = id, version = version, fail = fail)
        is Fail.Incident -> generateIncidentResponse(id = id, version = version, fail = fail)
    }

fun generateDataErrorResponse(id: UUID, version: ApiVersion, fail: DataErrors.Validation): ApiErrorResponse2 =
    ApiErrorResponse2(
        version = version,
        id = id,
        result = listOf(
            ApiErrorResponse2.Error(
                code = "${fail.code}/${GlobalProperties.service.id}",
                description = fail.description,
                details = ApiErrorResponse2.Error.Detail.tryCreateOrNull(name = fail.name).toList()
            )
        )
    )

fun generateErrorResponse(id: UUID, version: ApiVersion, fail: Fail.Error): ApiErrorResponse2 =
    ApiErrorResponse2(
        version = version,
        id = id,
        result = listOf(
            ApiErrorResponse2.Error(
                code = "${fail.code}/${GlobalProperties.service.id}",
                description = fail.description
            )
        )
    )

fun generateValidationErrorResponse(id: UUID, version: ApiVersion, fail: ValidationErrors): ApiErrorResponse2 =
    ApiErrorResponse2(
        version = version,
        id = id,
        result = listOf(
            ApiErrorResponse2.Error(
                code = "${fail.code}/${GlobalProperties.service.id}",
                description = fail.description,
                details = ApiErrorResponse2.Error.Detail.tryCreateOrNull(id = fail.entityId).toList()
            )
        )
    )

fun generateIncidentResponse(id: UUID, version: ApiVersion, fail: Fail.Incident): ApiIncidentResponse2 =
    ApiIncidentResponse2(
        id = id,
        version = version,
        result = ApiIncidentResponse2.Incident(
            id = UUID.randomUUID(),
            date = LocalDateTime.now(),
            level = fail.level,
            details = listOf(
                ApiIncidentResponse2.Incident.Detail(
                    code = "${fail.code}/${GlobalProperties.service.id}",
                    description = fail.description,
                    metadata = null
                )
            ),
            service = ApiIncidentResponse2.Incident.Service(
                id = GlobalProperties.service.id,
                version = GlobalProperties.service.version,
                name = GlobalProperties.service.name
            )
        )
    )

val NaN: UUID
    get() = UUID(0, 0)

fun JsonNode.getId(): Result<UUID, DataErrors> {
    return this.tryGetStringAttribute("id")
        .bind { text ->
            asUUID(text)
        }
}

fun JsonNode.getVersion(): Result<ApiVersion, DataErrors> {
    return this.tryGetStringAttribute("version")
        .bind { version ->
            when (val result = ApiVersion.tryOf(version)) {
                is Result.Success -> result
                is Result.Failure -> result.mapError {
                    DataErrors.Validation.DataFormatMismatch(
                        name = "version",
                        actualValue = version,
                        expectedFormat = "00.00.00"
                    )
                }
            }
        }
}

fun JsonNode.getAction(): Result<Command2Type, DataErrors> {
    return this.tryGetEnumAttribute(name = "action", enumProvider = Command2Type)
}

private fun <T> JsonNode.tryGetEnumAttribute(name: String, enumProvider: EnumElementProvider<T>)
    : Result<T, DataErrors> where T : Enum<T>,
                                  T : EnumElementProvider.Key =
    this.tryGetStringAttribute(name)
        .bind { enum ->
            enumProvider.orNull(enum)
                ?.asSuccess<T, DataErrors>()
                ?: failure(
                    DataErrors.Validation.UnknownValue(
                        name = name,
                        expectedValues = enumProvider.allowedElements.keysAsStrings(),
                        actualValue = enum
                    )
                )
        }

private fun JsonNode.tryGetStringAttribute(name: String): Result<String, DataErrors> {
    return this.tryGetAttribute(name = name, type = JsonNodeType.STRING)
        .map {
            it.asText()
        }
}

private fun JsonNode.tryGetAttribute(name: String, type: JsonNodeType): Result<JsonNode, DataErrors> =
    getAttribute(name = name)
        .bind { node ->
            if (node.nodeType == type)
                success(node)
            else
                failure(
                    DataErrors.Validation.DataTypeMismatch(
                        name = name,
                        expectedType = type.name,
                        actualType = node.nodeType.name
                    )
                )
        }

private fun JsonNode.getAttribute(name: String): Result<JsonNode, DataErrors> {
    return if (has(name)) {
        val attr = get(name)
        if (attr !is NullNode)
            success(attr)
        else
            failure(
                DataErrors.Validation.DataTypeMismatch(name = "$attr", actualType = "null", expectedType = "not null")
            )
    } else
        failure(DataErrors.Validation.MissingRequiredAttribute(name = name))
}

private fun asUUID(value: String): Result<UUID, DataErrors> =
    try {
        success<UUID>(UUID.fromString(value))
    } catch (exception: IllegalArgumentException) {
        failure(
            DataErrors.Validation.DataFormatMismatch(
                name = "id",
                expectedFormat = "uuid",
                actualValue = value
            )
        )
    }

fun <T : Any> JsonNode.tryGetParams(target: Class<T>): Result<T, Fail.Error> {
    val name = "params"
    return getAttribute(name).bind {
        when (val result = it.tryToObject(target)) {
            is Result.Success -> result
            is Result.Failure -> failure(
                BadRequestErrors.Parsing(
                    message = "Can not parse 'params'.",
                    request = this.toString(),
                    exception = result.error.exception
                )
            )
        }
    }
}