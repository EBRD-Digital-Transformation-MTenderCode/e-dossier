package com.procurement.dossier.infrastructure.converter

import com.procurement.dossier.application.service.params.ValidateRequirementResponseParams
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.infrastructure.handler.validate.requirementresponse.ValidateRequirementResponseRequest

fun ValidateRequirementResponseRequest.convert(): Result<ValidateRequirementResponseParams, DataErrors> =
    ValidateRequirementResponseParams.tryCreate(
        id = this.id,
        requirementId = this.requirementId,
        ocid = this.ocid,
        cpid = this.cpid,
        value = this.value
    )
