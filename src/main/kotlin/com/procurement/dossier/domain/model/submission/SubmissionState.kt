package com.procurement.dossier.domain.model.submission

import com.procurement.dossier.domain.model.enums.SubmissionStatus

data class SubmissionState(
    val id: SubmissionId,
    val status: SubmissionStatus
)