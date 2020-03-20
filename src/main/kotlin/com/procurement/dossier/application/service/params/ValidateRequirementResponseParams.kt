package com.procurement.dossier.application.service.params

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess

class ValidateRequirementResponseParams private constructor(
    val cpid: Cpid,
    val id: String,
    val ocid: Ocid,
    val requirementId: String,
    val value: RequirementRsValue
) {
    companion object {
        fun tryCreate(
            cpid: String,
            id: String,
            ocid: String,
            requirementId: String,
            value: RequirementRsValue
        ): Result<ValidateRequirementResponseParams, DataErrors> {
            val cpidResult = Cpid.tryCreate(value = cpid)
                .doOnError { pattert ->
                    return DataErrors.Validation.DataMismatchToPattern(
                        actualValue = cpid,
                        name = "cpid",
                        pattern = pattert
                    ).asFailure()
                }
                .get

            val ocidResult = Ocid.tryCreate(value = ocid)
                .doOnError { pattert ->
                    return DataErrors.Validation.DataMismatchToPattern(
                        actualValue = ocid,
                        name = "ocid",
                        pattern = pattert
                    ).asFailure()
                }
                .get

            return ValidateRequirementResponseParams(
                id = id,
                cpid = cpidResult,
                ocid = ocidResult,
                requirementId = requirementId,
                value = value
            ).asSuccess()
        }
    }
}