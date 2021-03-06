package com.procurement.dossier.domain.model.submission

import com.procurement.dossier.domain.model.Owner
import com.procurement.dossier.domain.model.Token

data class SubmissionCredentials(
    val id: SubmissionId,
    val token: Token,
    val owner: Owner
)