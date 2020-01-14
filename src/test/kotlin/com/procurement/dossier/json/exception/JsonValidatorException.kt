package com.procurement.dossier.json.exception

sealed class JsonException(message: String, exception: Exception? = null) :
    RuntimeException(message, exception)

class JsonFileNotFoundException(message: String) : JsonException(message)

class JsonCompareException(message: String) : JsonException("Error compare JSONs:\n$message")

class JsonBindingException(message: String, exception: Exception) : JsonException(message, exception)

class JsonMappingException(message: String, exception: Exception) : JsonException(message, exception)

class JsonParsingException(message: String, exception: Exception) : JsonException(message, exception)
