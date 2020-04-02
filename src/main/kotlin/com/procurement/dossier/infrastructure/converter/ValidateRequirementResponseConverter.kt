package com.procurement.dossier.infrastructure.converter

import com.procurement.dossier.application.service.params.ValidateRequirementResponseParams
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.infrastructure.handler.validate.requirementresponse.ValidateRequirementResponseRequest

fun ValidateRequirementResponseRequest.convert(): Result<ValidateRequirementResponseParams, DataErrors> =
    ValidateRequirementResponseParams.tryCreate(
        requirementResponse = requirementResponse.convert()
            .doReturn { error -> return failure(error) },
        ocid = ocid,
        cpid = cpid
    )

fun ValidateRequirementResponseRequest.RequirementResponse.convert():
    Result<ValidateRequirementResponseParams.RequirementResponse, DataErrors> =
    ValidateRequirementResponseParams.RequirementResponse.tryCreate(
        id = id,
        value = value,
        requirement = requirement.convert()
            .doReturn { error -> return failure(error) }
    )

fun ValidateRequirementResponseRequest.RequirementResponse.Requirement.convert():
    Result<ValidateRequirementResponseParams.RequirementResponse.Requirement, DataErrors> =
    ValidateRequirementResponseParams.RequirementResponse.Requirement.tryCreate(
        id = id
    )
