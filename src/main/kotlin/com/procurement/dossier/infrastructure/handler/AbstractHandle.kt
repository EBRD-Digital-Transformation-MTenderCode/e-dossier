package com.procurement.dossier.infrastructure.handler

import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.util.Action
import com.procurement.dossier.infrastructure.dto.ApiResponse2
import com.procurement.dossier.infrastructure.dto.ApiVersion
import com.procurement.dossier.infrastructure.model.dto.bpe.generateDataErrorResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.generateErrorResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.generateIncidentResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.generateValidationErrorResponse
import java.util.*

abstract class AbstractHandler<ACTION : Action, R : Any>(
    private val logger: Logger
) : Handler<ACTION, ApiResponse2> {

    protected fun responseError(id: UUID, version: ApiVersion, fail: Fail): ApiResponse2 {
        fail.logging(logger)

        return when (fail) {
            is Fail.Error -> {
                when (fail) {
                    is DataErrors.Validation -> generateDataErrorResponse(id = id, version = version, fail = fail)
                    is ValidationErrors      -> generateValidationErrorResponse(id = id, version = version, fail = fail)
                    else                     -> generateErrorResponse(id = id, version = version, fail = fail)
                }
            }
            is Fail.Incident -> generateIncidentResponse(id = id, version = version, fail = fail)
        }
    }
}
