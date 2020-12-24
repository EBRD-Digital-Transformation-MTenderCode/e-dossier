package com.procurement.dossier.infrastructure.exception

class EmptyStringException(val attributeName: String) : RuntimeException(attributeName)