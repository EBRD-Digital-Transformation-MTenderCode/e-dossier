package com.procurement.dossier.infrastructure.extension.cassandra

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.procurement.dossier.infrastructure.exception.io.ReadEntityException
import com.procurement.dossier.infrastructure.exception.io.SaveEntityException

fun BoundStatement.executeRead(session: Session, errorMessage: String): ResultSet = try {
    session.execute(this)
} catch (expected: Exception) {
    throw ReadEntityException(message = errorMessage, cause = expected)
}

fun BoundStatement.executeWrite(session: Session, errorMessage: String): ResultSet = try {
    session.execute(this)
} catch (expected: Exception) {
    throw SaveEntityException(message = errorMessage, cause = expected)
}