package com.procurement.dossier.infrastructure.converter

import com.procurement.dossier.application.model.PersonesProcessingParams
import com.procurement.dossier.application.model.notEmptyRule
import com.procurement.dossier.application.model.parseBusinessFunctionType
import com.procurement.dossier.application.model.parseCpid
import com.procurement.dossier.application.model.parseDate
import com.procurement.dossier.application.model.parseDocumentType
import com.procurement.dossier.application.model.parseOcid
import com.procurement.dossier.application.model.parsePersonId
import com.procurement.dossier.application.model.parsePersonTitle
import com.procurement.dossier.application.model.parseRole
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PartyRole
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.extension.mapResult
import com.procurement.dossier.domain.util.validate
import com.procurement.dossier.infrastructure.model.dto.request.PersonesProcessingRequest

val allowedRoles = PartyRole.allowedElements

    .filter {
        when (it) {
            PartyRole.INVITED_CANDIDATE -> true
            PartyRole.BUYER,
            PartyRole.PROCURING_ENTITY,
            PartyRole.CLIENT,
            PartyRole.CENTRAL_PURCHASING_BODY,
            PartyRole.AUTHOR,
            PartyRole.CANDIDATE,
            PartyRole.ENQUIRER,
            PartyRole.FUNDER,
            PartyRole.INVITED_TENDERER,
            PartyRole.PAYEE,
            PartyRole.PAYER,
            PartyRole.REVIEW_BODY,
            PartyRole.SUPPLIER,
            PartyRole.TENDERER -> false
        }
    }.toSet()

fun PersonesProcessingRequest.convert(): Result<PersonesProcessingParams, DataErrors> {
    val parties = parties.validate(notEmptyRule("parties"))
        .orForwardFail { return it }

    return PersonesProcessingParams(
        cpid = parseCpid(cpid).orForwardFail { return it },
        ocid = parseOcid(ocid).orForwardFail { return it },
        role = parseRole(role, allowedRoles, "role").orForwardFail { return it },
        parties = parties.mapResult { it.convert() }.orForwardFail { return it }
    ).asSuccess()
}

private fun PersonesProcessingRequest.Party.convert(): Result<PersonesProcessingParams.Party, DataErrors> {
    val persones = persones.validate(notEmptyRule("parties.persones"))
        .orForwardFail { return it }
        .mapResult { it.convert() }
        .orForwardFail { return it }

    return PersonesProcessingParams.Party(
        id = id,
        persones = persones
    ).asSuccess()
}

val allowedPersonTitles = PersonTitle.allowedElements.toSet()

private fun PersonesProcessingRequest.Party.Persone.convert(): Result<PersonesProcessingParams.Party.Persone, DataErrors> {
    val businessFunctions = businessFunctions.validate(notEmptyRule("parties.persones.businessFunctions"))
        .orForwardFail { return it }
        .mapResult { it.convert() }
        .orForwardFail { return it }

    return PersonesProcessingParams.Party.Persone(
        id = parsePersonId(id, "parties.persones.id").orForwardFail { return it },
        title = parsePersonTitle(title, allowedPersonTitles, "parties.persones.title").orForwardFail { return it },
        name = name,
        identifier = identifier.let { identifier ->
            PersonesProcessingParams.Party.Persone.Identifier(
                id = identifier.id,
                scheme = identifier.scheme,
                uri = identifier.uri
            )
        },
        businessFunctions = businessFunctions
    ).asSuccess()
}

val allowedBusinessFunctionType = BusinessFunctionType.allowedElements
    .filter {
        when (it) {
            BusinessFunctionType.AUTHORITY,
            BusinessFunctionType.CONTACT_POINT -> true
        }
    }
    .toSet()

private fun PersonesProcessingRequest.Party.Persone.BusinessFunction.convert(): Result<PersonesProcessingParams.Party.Persone.BusinessFunction, DataErrors> {
    val documents = documents.validate(notEmptyRule("parties.persones.businessFunctions.documents"))
        .orForwardFail { return it }
        ?.mapResult { it.convert() }
        ?.orForwardFail { return it }

    return PersonesProcessingParams.Party.Persone.BusinessFunction(
        id = id,
        jobTitle = jobTitle,
        type = parseBusinessFunctionType(type, allowedBusinessFunctionType, "parties.persones.businessFunctions.type")
            .orForwardFail { return it },
        period = PersonesProcessingParams.Party.Persone.BusinessFunction.Period(
            startDate = parseDate(period.startDate, "parties.persones.businessFunctions.period.startDate")
                .orForwardFail { return it }
        ),
        documents = documents
    ).asSuccess()
}

val allowedDocumentTypes = DocumentType.allowedElements
    .filter {
        when (it) {
            DocumentType.REGULATORY_DOCUMENT -> true
            DocumentType.ILLUSTRATION,
            DocumentType.SUBMISSION_DOCUMENTS,
            DocumentType.X_ELIGIBILITY_DOCUMENTS,
            DocumentType.X_QUALIFICATION_DOCUMENTS,
            DocumentType.X_TECHNICAL_DOCUMENTS -> false
        }
    }
    .toSet()

private fun PersonesProcessingRequest.Party.Persone.BusinessFunction.Document.convert(): Result<PersonesProcessingParams.Party.Persone.BusinessFunction.Document, DataErrors> =
    PersonesProcessingParams.Party.Persone.BusinessFunction.Document(
        id = id,
        title = title,
        description = description,
        documentType = parseDocumentType(
            documentType,
            allowedDocumentTypes,
            "parties.persones.businessFunctions.documents.documentType"
        )
            .orForwardFail { return it }
    ).asSuccess()

