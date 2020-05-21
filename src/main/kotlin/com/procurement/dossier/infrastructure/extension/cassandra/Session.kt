package com.procurement.dossier.infrastructure.extension.cassandra

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.Result.Companion.failure
import com.procurement.dossier.domain.util.Result.Companion.success
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

fun BoundStatement.tryExecute(session: Session): Result<ResultSet, Fail.Incident.Database> = try {
    success(session.execute(this))
} catch (expected: Exception) {
    failure(Fail.Incident.Database(exception = expected))
}