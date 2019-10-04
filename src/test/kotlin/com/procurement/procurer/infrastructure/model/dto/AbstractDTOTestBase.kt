package com.procurement.procurer.infrastructure.model.dto

import com.procurement.procurer.json.testingBindingAndMapping

abstract class AbstractDTOTestBase<T : Any>(private val target: Class<T>) {
    fun testBindingAndMapping(pathToJsonFile: String) {
        testingBindingAndMapping(pathToJsonFile, target)
    }
}
