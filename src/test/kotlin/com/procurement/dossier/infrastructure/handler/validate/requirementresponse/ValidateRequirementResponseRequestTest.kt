package com.procurement.dossier.infrastructure.handler.validate.requirementresponse

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import org.junit.jupiter.api.Test

class ValidateRequirementResponseRequestTest : AbstractDTOTestBase<ValidateRequirementResponseRequest>(
    ValidateRequirementResponseRequest::class.java
) {
    @Test
    fun test() {
        testBindingAndMapping("json/handler/validate/requirementresponse/validate_requirement_response_request_full.json")
    }
}
