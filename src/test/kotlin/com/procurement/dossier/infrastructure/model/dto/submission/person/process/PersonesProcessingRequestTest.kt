package com.procurement.dossier.infrastructure.model.dto.submission.person.process

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.request.PersonesProcessingRequest
import org.junit.jupiter.api.Test

internal class PersonesProcessingRequestTest : AbstractDTOTestBase<PersonesProcessingRequest>(PersonesProcessingRequest::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/person/process/persones_processing_request_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/dto/submission/person/process/persones_processing_request_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/dto/submission/person/process/persones_processing_request_required_2.json")
    }
}