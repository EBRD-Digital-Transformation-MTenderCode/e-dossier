package com.procurement.dossier.json

import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod

object JsonFilePathGenerator {

    fun pmdSegment(pmd: ProcurementMethod): String = when (pmd) {
        ProcurementMethod.OT, ProcurementMethod.TEST_OT,
        ProcurementMethod.SV, ProcurementMethod.TEST_SV,
        ProcurementMethod.MV, ProcurementMethod.TEST_MV -> "op"

        ProcurementMethod.DA, ProcurementMethod.TEST_DA,
        ProcurementMethod.NP, ProcurementMethod.TEST_NP,
        ProcurementMethod.OP, ProcurementMethod.TEST_OP -> "lp"

        ProcurementMethod.RT, ProcurementMethod.TEST_RT,
        ProcurementMethod.FA, ProcurementMethod.TEST_FA -> throw IllegalArgumentException()
    }

    fun auctionSegment(hasAuctions: Boolean): String =
        if (hasAuctions) "with_auctions" else "without_auctions"

    fun itemsSegment(hasItems: Boolean): String =
        if (hasItems) "with_items" else "without_items"

    fun documentsSegment(hasDocuments: Boolean): String =
        if (hasDocuments) "with_documents" else "without_documents"
}