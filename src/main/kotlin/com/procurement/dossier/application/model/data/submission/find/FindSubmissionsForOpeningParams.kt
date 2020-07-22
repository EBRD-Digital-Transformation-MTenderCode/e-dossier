package com.procurement.dossier.application.model.data.submission.find

import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parseOperationType
import com.procurement.dossier.application.model.parsePmd
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.infrastructure.model.dto.ocds.Operation
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod

class FindSubmissionsForOpeningParams private constructor(
    val cpid: Cpid,
    val ocid: Ocid,
    val pmd: ProcurementMethod,
    val country: String,
    val operationType: Operation
) {
    companion object {
        val allowedPmd = ProcurementMethod.allowedElements
            .filter { value ->
                when (value) {
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA -> true
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> false
                }
            }

        val allowedOperationType = Operation.allowedElements
            .filter { value ->
                when (value) {
                    Operation.START_SECOND_STAGE -> true
                    Operation.CREATE_CN,
                    Operation.CREATE_CN_ON_PIN,
                    Operation.CREATE_CN_ON_PN,
                    Operation.CREATE_NEGOTIATION_CN_ON_PN,
                    Operation.CREATE_PIN,
                    Operation.CREATE_PIN_ON_PN,
                    Operation.CREATE_PN,
                    Operation.UPDATE_CN,
                    Operation.UPDATE_PN -> false
                }
            }

        fun tryCreate(
            cpid: String, ocid: String, pmd: String, country: String, operationType: String
        ): Result<FindSubmissionsForOpeningParams, DataErrors> {
            val cpidParsed = parseCpid(cpid)
                .orForwardFail { fail -> return fail }
            val ocidParsed = parseOcid(ocid)
                .orForwardFail { fail -> return fail }
            val pmdParsed = parsePmd(value = pmd, allowedValues = allowedPmd)
                .orForwardFail { fail -> return fail }
            val operationTypeParsed = parseOperationType(value = operationType, allowedEnums = allowedOperationType)
                .orForwardFail { fail -> return fail }

            return FindSubmissionsForOpeningParams(
                cpid = cpidParsed,
                ocid = ocidParsed,
                country = country,
                pmd = pmdParsed,
                operationType = operationTypeParsed
            ).asSuccess()
        }
    }
}