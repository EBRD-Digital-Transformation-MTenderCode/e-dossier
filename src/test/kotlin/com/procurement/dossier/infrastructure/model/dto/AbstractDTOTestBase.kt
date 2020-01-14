package com.procurement.dossier.infrastructure.model.dto

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.json.testingBindingAndMapping

abstract class AbstractDTOTestBase<T : Any>(private val target: Class<T>) {
    fun testBindingAndMapping(pathToJsonFile: String) {
        testingBindingAndMapping(pathToJsonFile, target)
    }
    fun testBindingAndMapping(jsonNode: JsonNode) {
        testingBindingAndMapping(jsonNode, target)
    }
}
