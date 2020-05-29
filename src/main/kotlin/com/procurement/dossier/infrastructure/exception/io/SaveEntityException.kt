package com.procurement.dossier.infrastructure.exception.io

class SaveEntityException : RuntimeException {
    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
