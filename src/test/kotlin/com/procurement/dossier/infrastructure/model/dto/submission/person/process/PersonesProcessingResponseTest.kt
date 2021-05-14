package com.procurement.dossier.infrastructure.model.dto.submission.person.process

import com.procurement.dossier.infrastructure.model.dto.AbstractDTOTestBase
import com.procurement.dossier.infrastructure.model.dto.response.PersonesProcessingResponse
import org.junit.jupiter.api.Test

internal class PersonesProcessingResponseTest : AbstractDTOTestBase<PersonesProcessingResponse>(PersonesProcessingResponse::class.java) {

    @Test
    fun fully() {
        testBindingAndMapping("json/dto/submission/person/process/persones_processing_response_full.json")
    }

    @Test
    fun required_1() {
        testBindingAndMapping("json/dto/submission/person/process/persones_processing_response_required_1.json")
    }

    @Test
    fun required_2() {
        testBindingAndMapping("json/dto/submission/person/process/persones_processing_response_required_2.json")
    }
}