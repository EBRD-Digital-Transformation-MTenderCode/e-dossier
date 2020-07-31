package com.procurement.dossier.infrastructure.repository

import com.datastax.driver.core.Session
import com.procurement.dossier.application.repository.RulesRepository
import com.procurement.dossier.domain.EnumElementProvider
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.tryToLong
import com.procurement.dossier.infrastructure.exception.io.ReadEntityException
import com.procurement.dossier.infrastructure.extension.cassandra.tryExecute
import com.procurement.dossier.infrastructure.model.dto.ocds.Operation
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class CassandraRulesRepository(private val session: Session) : RulesRepository {
    companion object {
        private const val keySpace = "dossier"
        private const val tableName = "rules"
        private const val columnCountry = "country"
        private const val columnPmd = "pmd"
        private const val columnOperationType = "operation_type"
        private const val columnParameter = "parameter"
        private const val columnValue = "value"

        private const val FIND_PARAMETER_VALUE_CQL = """
               SELECT $columnValue
                 FROM $keySpace.$tableName
                WHERE $columnCountry=? 
                  AND $columnPmd=?
                  AND $columnOperationType =?
                  AND $columnParameter=?
            """

        private const val OPERATION_TYPE_ALL = "all"

        enum class Parameter(override val key: String) : EnumElementProvider.Key {
            PERIOD_DURATION_PARAMETER("minSubmissionPeriodDuration"),
            SUBMISSIONS_MINIMUM_PARAMETER("minQtySubmissionsForReturning"),
            EXTENSION_PARAMETER("extensionAfterUnsuspended"),
            VALID_STATES_PARAMETER("validStates")
        }
    }

    private val preparedFindParameterValueCQL = session.prepare(FIND_PARAMETER_VALUE_CQL)

    override fun findExtensionAfterUnsuspended(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation?
    ): Duration? = findParameter(country, pmd, operationType, Parameter.EXTENSION_PARAMETER)
        .orThrow { fail -> throw ReadEntityException(message = fail.message, cause = fail.exception) }
        ?.tryToLong()
        ?.orThrow { fail ->
            throw ReadEntityException(message = fail.message, cause = fail.exception)
        }
        ?.let { Duration.ofSeconds(it) }

    override fun findPeriodDuration(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation?
    ): Duration? = findParameter(country, pmd, operationType, Parameter.PERIOD_DURATION_PARAMETER)
        .orThrow { fail -> throw ReadEntityException(message = fail.message, cause = fail.exception) }
        ?.tryToLong()
        ?.orThrow { fail ->
            throw ReadEntityException(message = fail.message, cause = fail.exception)
        }
        ?.let { Duration.ofSeconds(it) }

    override fun findSubmissionsMinimumQuantity(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation
    ): Result<Long?, Fail.Incident.Database> {
        val parameterValue = findParameter(country, pmd, operationType, Parameter.SUBMISSIONS_MINIMUM_PARAMETER)
            .orForwardFail { fail -> return fail }

        return parameterValue
            ?.tryToLong()
            ?.doReturn { incident ->
                return Fail.Incident.Database.Parsing(
                    column = columnValue,
                    value = parameterValue,
                    exception = incident.exception
                ).asFailure()
            }
            .asSuccess()
    }

    override fun findSubmissionValidState(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation
    ): Result<SubmissionStatus?, Fail.Incident.Database> {
        val parameterValue = findParameter(country, pmd, operationType, Parameter.VALID_STATES_PARAMETER)
            .orForwardFail { fail -> return fail }

        return parameterValue
            ?.let {
                SubmissionStatus.tryOf(it)
                    .doReturn {
                        return Fail.Incident.Database.Parsing(column = columnValue, value = it).asFailure()
                    }
            }
            .asSuccess()
    }

    fun findParameter(
        country: String,
        pmd: ProcurementMethod,
        operationType: Operation?,
        parameter: Parameter
    ): Result<String?, Fail.Incident.Database.Interaction> = preparedFindParameterValueCQL.bind()
        .apply {
            setString(columnCountry, country)
            setString(columnPmd, pmd.name)
            setString(columnOperationType, operationType?.key ?: OPERATION_TYPE_ALL)
            setString(columnParameter, parameter.key)
        }
        .tryExecute(session)
        .orForwardFail { fail -> return fail }
        .one()
        ?.getString(columnValue)
        .asSuccess()
}
