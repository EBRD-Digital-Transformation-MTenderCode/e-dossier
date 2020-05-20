package com.procurement.dossier.application.service.params

import com.procurement.dossier.application.model.data.RequirementRsValue
import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseRequirementId
import com.procurement.dossier.application.model.parseRequirementResponseId
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.requirement.RequirementId
import com.procurement.dossier.domain.model.requirement.response.RequirementResponseId
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess

class ValidateRequirementResponseParams private constructor(
    val cpid: Cpid,
    val requirementResponse: RequirementResponse
) {
    companion object {
        fun tryCreate(cpid: String, requirementResponse: RequirementResponse)
            : Result<ValidateRequirementResponseParams, DataErrors> {

            val cpidParsed = parseCpid(value = cpid)
                .doReturn { error -> return error.asFailure() }

            return ValidateRequirementResponseParams(cpid = cpidParsed, requirementResponse = requirementResponse)
                .asSuccess()
        }
    }

    class RequirementResponse private constructor(
        val id: RequirementResponseId,
        val value: RequirementRsValue,
        val requirement: Requirement
    ) {
        companion object {
            fun tryCreate(
                id: String,
                value: RequirementRsValue,
                requirement: Requirement
            ): Result<RequirementResponse, DataErrors> {

                val idParsed = parseRequirementResponseId(id)
                    .doReturn { error -> return error.asFailure() }

                return RequirementResponse(id = idParsed, value = value, requirement = requirement)
                    .asSuccess()
            }
        }

        data class Requirement(val id: RequirementId) {

            companion object {
                fun tryCreate(id: String): Result<Requirement, DataErrors> {

                    val parsedId = parseRequirementId(id = id)
                        .doReturn { error -> return Result.failure(error) }

                    return Requirement(parsedId)
                        .asSuccess()
                }
            }
        }
    }
}