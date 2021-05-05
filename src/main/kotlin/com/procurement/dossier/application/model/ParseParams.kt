package com.procurement.dossier.application.model

import com.procurement.dossier.domain.EnumElementProvider
import com.procurement.dossier.domain.EnumElementProvider.Companion.keysAsStrings
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.fail.error.DataTimeError
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.Owner
import com.procurement.dossier.domain.model.Token
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.document.tryDocumentId
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PartyRole
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.model.enums.Scale
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.enums.SupplierType
import com.procurement.dossier.domain.model.person.PersonId
import com.procurement.dossier.domain.model.qualification.QualificationId
import com.procurement.dossier.domain.model.qualification.QualificationStatus
import com.procurement.dossier.domain.model.requirement.RequirementId
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import com.procurement.dossier.domain.model.requirement.response.tryRequirementResponseId
import com.procurement.dossier.domain.model.requirement.tryRequirementId
import com.procurement.dossier.domain.model.tryOwner
import com.procurement.dossier.domain.model.tryToken
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.tryParseLocalDateTime
import com.procurement.dossier.infrastructure.model.dto.ocds.Operation
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import java.time.LocalDateTime

fun parseCpid(value: String): Result<Cpid, DataErrors.Validation.DataMismatchToPattern> =
    Cpid.tryCreateOrNull(value = value)
        ?.asSuccess()
        ?: Result.failure(
            DataErrors.Validation.DataMismatchToPattern(
                name = "cpid",
                pattern = Cpid.pattern,
                actualValue = value
            )
        )

fun parseOcid(value: String): Result<Ocid, DataErrors.Validation.DataMismatchToPattern> =
    Ocid.tryCreateOrNull(value = value)
        ?.asSuccess()
        ?: Result.failure(
            DataErrors.Validation.DataMismatchToPattern(
                name = "ocid",
                pattern = Ocid.pattern,
                actualValue = value
            )
        )

fun parsePmd(
    value: String,
    allowedValues: Collection<ProcurementMethod>
): Result<ProcurementMethod, DataErrors.Validation.UnknownValue> =
    try {
        ProcurementMethod.valueOf(value)
            .takeIf { it in allowedValues }
            ?.asSuccess()
            ?: getPmdDataErrorFailure(value, allowedValues)
    } catch (ignored: Exception) {
        getPmdDataErrorFailure(value, allowedValues)
    }

private fun getPmdDataErrorFailure(value: String, allowedValues: Collection<ProcurementMethod>) =
    Result.failure(
        DataErrors.Validation.UnknownValue(
            name = "pmd",
            expectedValues = allowedValues.map { it.name },
            actualValue = value
        )
    )

fun parseOwner(value: String): Result<Owner, DataErrors.Validation.DataFormatMismatch> =
    value.tryOwner()
        .doReturn {
            return Result.failure(
                DataErrors.Validation.DataFormatMismatch(
                    name = "owner",
                    expectedFormat = "uuid",
                    actualValue = value
                )
            )
        }.asSuccess()

fun parseToken(value: String): Result<Token, DataErrors.Validation.DataFormatMismatch> =
    value.tryToken()
        .doReturn {
            return Result.failure(
                DataErrors.Validation.DataFormatMismatch(
                    name = "token",
                    expectedFormat = "uuid",
                    actualValue = value
                )
            )
        }.asSuccess()

fun parseRequirementId(id: String): Result<RequirementId, DataErrors> =
    id.tryRequirementId()
        .doReturn {
            return Result.failure(
                DataErrors.Validation.DataFormatMismatch(
                    name = "requirementResponse.requirement.id",
                    expectedFormat = "string",
                    actualValue = id
                )
            )
        }.asSuccess()

fun parseRequirementResponseId(id: String): Result<RequirementResponseId, DataErrors> =
    id.tryRequirementResponseId()
        .doReturn {
            return Result.failure(
                DataErrors.Validation.DataFormatMismatch(
                    name = "requirementResponse.id",
                    expectedFormat = "string",
                    actualValue = id
                )
            )
        }.asSuccess()

fun parseDocumentId(
    value: String,
    attributeName: String
): Result<DocumentId, DataErrors.Validation.DataFormatMismatch> =
    value.tryDocumentId()
        .doReturn {
            return Result.failure(
                DataErrors.Validation.DataFormatMismatch(
                    name = attributeName,
                    expectedFormat = "string",
                    actualValue = value
                )
            )
        }.asSuccess()

fun parseQualificationId(
    value: String, attributeName: String
): Result<QualificationId, DataErrors.Validation.DataMismatchToPattern> {
    val id = QualificationId.tryCreateOrNull(value)
        ?: return Result.failure(
            DataErrors.Validation.DataMismatchToPattern(
                name = attributeName,
                pattern = QualificationId.pattern,
                actualValue = value
            )
        )
    return id.asSuccess()
}

fun parsePersonId(value: String, attributeName: String): Result<PersonId, DataErrors.Validation.DataFormatMismatch> =
    PersonId.tryCreate(value)
        .mapError {
            DataErrors.Validation.DataFormatMismatch(
                name = attributeName,
                actualValue = value,
                expectedFormat = "string"
            )
        }

fun parsePersonTitle(
    value: String, allowedEnums: Set<PersonTitle>, attributeName: String
): Result<PersonTitle, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = PersonTitle)

fun parseBusinessFunctionType(
    value: String, allowedEnums: Set<BusinessFunctionType>, attributeName: String
): Result<BusinessFunctionType, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = BusinessFunctionType)

fun parseDocumentType(
    value: String, allowedEnums: Set<DocumentType>, attributeName: String
): Result<DocumentType, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = DocumentType)

fun parseSupplierType(
    value: String, allowedEnums: Set<SupplierType>, attributeName: String
): Result<SupplierType, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = SupplierType)

fun parseScale(
    value: String, allowedEnums: Set<Scale>, attributeName: String
): Result<Scale, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = Scale)

fun parseSubmissionStatus(
    value: String, allowedEnums: Set<SubmissionStatus>, attributeName: String
): Result<SubmissionStatus, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = SubmissionStatus)

fun parseQualificationStatus(
    value: String, allowedEnums: Set<QualificationStatus>, attributeName: String
): Result<QualificationStatus, DataErrors.Validation.UnknownValue> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = QualificationStatus)

fun parseOperationType(
    value: String, allowedEnums: Set<Operation>, attributeName: String = "operationType"
): Result<Operation, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = Operation)

fun parseRole(
    value: String, allowedEnums: Set<PartyRole>, attributeName: String
): Result<PartyRole, DataErrors> =
    parseEnum(value = value, allowedEnums = allowedEnums, attributeName = attributeName, target = PartyRole)

private fun <T> parseEnum(
    value: String, allowedEnums: Set<T>, attributeName: String, target: EnumElementProvider<T>
): Result<T, DataErrors.Validation.UnknownValue> where T : Enum<T>,
                                                       T : EnumElementProvider.Key =
    target.orNull(value)
        ?.takeIf { it in allowedEnums }
        ?.asSuccess()
        ?: Result.failure(
            DataErrors.Validation.UnknownValue(
                name = attributeName,
                expectedValues = allowedEnums.keysAsStrings(),
                actualValue = value
            )
        )

fun parseDate(value: String, attributeName: String): Result<LocalDateTime, DataErrors.Validation> =
    value.tryParseLocalDateTime()
        .mapError { fail ->
            when (fail) {
                is DataTimeError.InvalidFormat -> DataErrors.Validation.DataFormatMismatch(
                    name = attributeName,
                    actualValue = value,
                    expectedFormat = fail.pattern
                )

                is DataTimeError.InvalidDateTime ->
                    DataErrors.Validation.InvalidDateTime(name = attributeName, actualValue = value)
            }
        }
