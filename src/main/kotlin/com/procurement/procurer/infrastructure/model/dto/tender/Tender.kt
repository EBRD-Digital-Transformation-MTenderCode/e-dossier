package com.procurement.procurer.infrastructure.model.dto.tender

import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteria
import com.procurement.procurer.infrastructure.model.dto.ocds.AwardCriteriaDetails
import com.procurement.procurer.infrastructure.model.dto.ocds.MainProcurementCategory

data class Tender(
    var awardCriteria: AwardCriteria,
    var awardCriteriaDetails: AwardCriteriaDetails?,
    var criteria: List<Criteria>?,
    var conversions: List<Conversion>?,
    var items: List<Item>,
    val mainProcurementCategory: MainProcurementCategory
)
