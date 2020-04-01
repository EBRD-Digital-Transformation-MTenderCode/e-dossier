package com.procurement.dossier.application.service.params

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
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
            val cpidResult = parseCpid(value = cpid)
                .doOnError { error ->
                    return error.asFailure()
                }
                .get

            val ocidResult = parseOcid(value = ocid)
                .doOnError { error ->
                    return error.asFailure()
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