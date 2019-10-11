package com.procurement.procurer.json

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.procurer.infrastructure.utils.toJson
import com.procurement.procurer.json.exception.JsonCompareException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail

inline fun <reified T : Any> testingBindingAndMapping(pathToJsonFile: String) {
    testingBindingAndMapping(pathToJsonFile, T::class.java)
}

fun <T : Any> testingBindingAndMapping(pathToJsonFile: String, target: Class<T>) {
    val expected = loadJson(pathToJsonFile)
    val obj = expected.toObject(target)
    val actual = obj.toJson()

    try {
        JsonValidator.equalsJsons(expected, actual)
    } catch (exception: JsonCompareException) {
        fail<T>("Error testing binding and mapping JSON file by path: '$pathToJsonFile' to an object of type '${target.canonicalName}'.\n${exception.message}")
    }
}

fun <T : Any> testingBindingAndMapping(jsonNode: JsonNode, target: Class<T>) {
    val expected = jsonNode.toJson()
    val obj = expected.toObject(target)
    val actual = obj.toJson()
    println(toJson(jsonNode))
    try {
        JsonValidator.equalsJsons(expected, actual)
    } catch (exception: JsonCompareException) {
        fail<T>("Error testing binding and mapping JSON to an object of type '${target.canonicalName}'.\n${exception.message}")
    }
}

object JsonValidator {
    class JsonValues(private val valueByPaths: Map<String, String>) {
        fun assert(path: String, expectedValue: String) {
            val actualValue = getActualValue(path)
            equalsValues(path = path, expected = "\"$expectedValue\"", actual = actualValue)
        }

        fun assert(path: String, expectedValue: Boolean) {
            val actualValue = getActualValue(path)
            equalsValues(path = path, expected = expectedValue.toString(), actual = actualValue)
        }

        fun assert(path: String, expectedValue: Number) {
            val actualValue = getActualValue(path)
            equalsValues(path = path, expected = expectedValue.toString(), actual = actualValue)
        }

        private fun getActualValue(path: String): String {
            val actualValue = valueByPaths[path]
            return actualValue ?: Assertions.fail<String>("The path $path to check value not found.")
        }

        private fun equalsValues(path: String, expected: String, actual: String) {
            Assertions.assertEquals(
                expected,
                actual,
                "Invalid value by path $path (expected value: $expected, actual value: $actual)."
            )
        }
    }

    fun equalsJsons(expectedJson: String, actualJson: String, additionalChecks: (JsonValues.() -> Unit)? = null) {
        val expectedData: Map<String, String> = JsonPathParser.parse(expectedJson)
        val actualData: Map<String, String> = JsonPathParser.parse(actualJson)

        if (expectedData.size != actualData.size) {
            val intersect = expectedData.keys.intersect(actualData.keys)

            val message = buildString {
                appendln("Error comparing JSONs. Different number of attributes (expected json: ${expectedData.size} attributes, actual json: ${actualData.size} attributes)")
                for ((key, value) in expectedData) {
                    if (!intersect.contains(key))
                        appendln("EXPECTED JSON => path: $key, value: $value")
                }
                for ((key, value) in actualData) {
                    if (!intersect.contains(key))
                        appendln("ACTUAL JSON => path: $key, value: $value")
                }
            }
            throw JsonCompareException(message)
        }

        val message = buildString() {
            actualData.forEach { (path, value) ->
                val expectedValue = expectedData[path]
                if (expectedValue != null) {
                    if (expectedValue != value)
                        appendln("Actual value [$value] not equals expected value [$expectedValue] by path: $path.")
                } else {
                    appendln("Path $path not found in actual json.")
                }
            }
        }

        if (message.isNotBlank())
            throw JsonCompareException(message)


        if (additionalChecks != null) {
            try {
                additionalChecks(JsonValues(actualData))
            } catch (exception: Exception) {
                JsonCompareException(exception.message!!)
            }
        }
    }

    fun checkValueByPath(json: String, checks: JsonValues.() -> Unit) {
        val actualData: Map<String, String> = JsonPathParser.parse(json)
        checks(JsonValues(actualData))
    }
}
