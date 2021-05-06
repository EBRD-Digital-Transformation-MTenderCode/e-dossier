package com.procurement.dossier.application.model

import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.document.DocumentId
import com.procurement.dossier.domain.model.enums.BusinessFunctionType
import com.procurement.dossier.domain.model.enums.DocumentType
import com.procurement.dossier.domain.model.enums.PartyRole
import com.procurement.dossier.domain.model.enums.PersonTitle
import com.procurement.dossier.domain.model.person.PersonId
import java.time.LocalDateTime

data class PersonesProcessingParams(
    val cpid: Cpid,
    val ocid: Ocid,
    val role: PartyRole,
    val parties: List<Party>
) {
    data class Party(
        val id: String,
        val persones: List<Persone>
    ) {
        data class Persone(
            val id: PersonId,
            val name: String,
            val title: PersonTitle,
            val identifier: Identifier,
            val businessFunctions: List<BusinessFunction>
        ) {
            data class Identifier(
                val scheme: String,
                val id: String,
                val uri: String?
            )

            data class BusinessFunction(
                val id: String,
                val type: BusinessFunctionType,
                val jobTitle: String,
                val period: Period,
                val documents: List<Document>?
            ) {
                data class Period(
                    val startDate: LocalDateTime
                )

                data class Document(
                    val id: DocumentId,
                    val documentType: DocumentType,
                    val title: String,
                    val description: String?
                )
            }
        }
    }
}