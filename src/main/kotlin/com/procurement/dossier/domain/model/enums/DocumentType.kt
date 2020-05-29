package com.procurement.dossier.domain.model.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.dossier.domain.EnumElementProvider

enum class DocumentType(@JsonValue override val key: String) : EnumElementProvider.Key {

    REGULATORY_DOCUMENT("regulatoryDocument"),
    SUBMISSION_DOCUMENTS("submissionDocuments"),
    ILLUSTRATION("illustration"),
    X_QUALIFICATION_DOCUMENTS("x_qualificationDocuments"),
    X_ELIGIBILITY_DOCUMENTS("x_eligibilityDocuments"),
    X_TECHNICAL_DOCUMENTS("x_technicalDocuments"), ;

    override fun toString(): String = key

    companion object : EnumElementProvider<DocumentType>(info = info()) {

        @JvmStatic
        @JsonCreator
        fun creator(name: String) = DocumentType.orThrow(name)
    }
}
