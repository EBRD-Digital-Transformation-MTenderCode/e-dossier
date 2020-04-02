package com.procurement.dossier.application.model

import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.requirement.RequirementId
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import com.procurement.dossier.domain.model.requirement.response.tryRequirementResponseId
import com.procurement.dossier.domain.model.requirement.tryRequirementId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess

fun parseCpid(value: String): Result<Cpid, DataErrors.Validation.DataMismatchToPattern> =
    Cpid.tryCreate(value = value)
        .doOnError { expectedPattern ->
            return Result.failure(
                DataErrors.Validation.DataMismatchToPattern(
                    name = "cpid",
                    pattern = expectedPattern,
                    actualValue = value
                )
            )
        }
        .get
        .asSuccess()

fun parseOcid(value: String): Result<Ocid, DataErrors.Validation.DataMismatchToPattern> =
    Ocid.tryCreate(value = value)
        .doOnError { expectedPattern ->
            return Result.failure(
                DataErrors.Validation.DataMismatchToPattern(
                    name = "ocid",
                    pattern = expectedPattern,
                    actualValue = value
                )
            )
        }
        .get
        .asSuccess()

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

