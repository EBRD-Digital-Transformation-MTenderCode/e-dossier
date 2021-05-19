package com.procurement.dossier.infrastructure.converter.submission

import com.procurement.dossier.application.model.data.submission.get.GetInvitedCandidatesOwnersParams
import com.procurement.dossier.infrastructure.model.dto.request.GetInvitedCandidatesOwnersRequest

fun GetInvitedCandidatesOwnersRequest.convert() = GetInvitedCandidatesOwnersParams.tryCreate(cpid = cpid, ocid = ocid)