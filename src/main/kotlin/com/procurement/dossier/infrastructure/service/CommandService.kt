package com.procurement.dossier.infrastructure.service

import com.procurement.dossier.application.exception.ErrorException
import com.procurement.dossier.application.exception.ErrorType
import com.procurement.dossier.application.model.data.CreatedCriteria
import com.procurement.dossier.application.model.data.GetCriteriaData
import com.procurement.dossier.application.model.data.period.check.CheckPeriodContext
import com.procurement.dossier.application.model.data.period.extend.ExtendSubmissionPeriodContext
import com.procurement.dossier.application.model.data.period.save.SavePeriodContext
import com.procurement.dossier.application.model.data.period.validate.ValidatePeriodContext
import com.procurement.dossier.application.repository.history.HistoryDao
import com.procurement.dossier.application.service.CriteriaService
import com.procurement.dossier.application.service.PeriodService
import com.procurement.dossier.application.service.command.generateCreateCriteriaResponse
import com.procurement.dossier.application.service.context.CheckResponsesContext
import com.procurement.dossier.application.service.context.CreateCriteriaContext
import com.procurement.dossier.application.service.context.EvPanelsContext
import com.procurement.dossier.application.service.context.GetCriteriaContext
import com.procurement.dossier.infrastructure.converter.period.convert
import com.procurement.dossier.infrastructure.converter.toResponseDto
import com.procurement.dossier.infrastructure.dto.ApiSuccessResponse
import com.procurement.dossier.infrastructure.model.dto.bpe.CommandMessage
import com.procurement.dossier.infrastructure.model.dto.bpe.CommandType
import com.procurement.dossier.infrastructure.model.dto.bpe.country
import com.procurement.dossier.infrastructure.model.dto.bpe.cpid
import com.procurement.dossier.infrastructure.model.dto.bpe.cpidParsed
import com.procurement.dossier.infrastructure.model.dto.bpe.ocidCnParsed
import com.procurement.dossier.infrastructure.model.dto.bpe.ocidParsed
import com.procurement.dossier.infrastructure.model.dto.bpe.operationType
import com.procurement.dossier.infrastructure.model.dto.bpe.owner
import com.procurement.dossier.infrastructure.model.dto.bpe.pmd
import com.procurement.dossier.infrastructure.model.dto.bpe.startDate
import com.procurement.dossier.infrastructure.model.dto.ocds.ProcurementMethod
import com.procurement.dossier.infrastructure.model.dto.request.period.CheckPeriodRequest
import com.procurement.dossier.infrastructure.model.dto.request.period.SavePeriodRequest
import com.procurement.dossier.infrastructure.model.dto.request.period.ValidatePeriodRequest
import com.procurement.dossier.infrastructure.utils.toJson
import com.procurement.dossier.infrastructure.utils.toObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandService(
    private val historyDao: HistoryDao,
    private val criteriaService: CriteriaService,
    private val periodService: PeriodService
) {
    companion object {
        private val log = LoggerFactory.getLogger(CommandService::class.java)
    }

    fun execute(cm: CommandMessage): ApiSuccessResponse {
        val dataOfResponse: Any = when (cm.command) {
            CommandType.CHECK_CRITERIA -> {
                when (cm.pmd) {
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV ->
                        criteriaService.checkCriteria(cm)
                            .also {
                                log.debug("Checking criteria was a success.")
                            }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> throw ErrorException(ErrorType.INVALID_PMD)

                }
            }
            CommandType.CREATE_CRITERIA -> {
                when (cm.pmd) {
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> {
                        val context = CreateCriteriaContext(cpid = cm.cpid, owner = cm.owner)
                        criteriaService.createCriteria(cm, context = context)
                            .also { result: CreatedCriteria ->
                                if (log.isDebugEnabled)
                                    log.debug("Criteria was created. Result: ${toJson(result)}")
                            }
                            .let { result: CreatedCriteria ->
                                generateCreateCriteriaResponse(result)
                            }
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> throw ErrorException(ErrorType.INVALID_PMD)
                }
            }
            CommandType.CHECK_RESPONSES -> {
                when (cm.pmd) {
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> {
                        val context = CheckResponsesContext(cpid = cm.cpid, owner = cm.owner)
                        criteriaService.checkResponses(cm, context = context)
                            .also {
                                log.debug("Checking response was a success.")
                            }
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> throw ErrorException(ErrorType.INVALID_PMD)
                }
            }
            CommandType.GET_CRITERIA -> {
                when (cm.pmd) {
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> {
                        val context = GetCriteriaContext(cpid = cm.cpid)
                        criteriaService.getCriteriaDetails(context = context)
                            .also { result: GetCriteriaData? ->
                                if (result != null)
                                    log.debug("Getting criteria. Result: ${toJson(result)}")
                                else
                                    log.debug("No criteria.")
                            }
                            ?.toResponseDto()
                            ?: Unit
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> throw ErrorException(ErrorType.INVALID_PMD)
                }
            }
            CommandType.CREATE_REQUESTS_FOR_EV_PANELS -> {
                when (cm.pmd) {
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> {
                        val context = EvPanelsContext(cpid = cm.cpid, owner = cm.owner)
                        criteriaService.createRequestsForEvPanels(context = context)
                            .also { result ->
                                if (log.isDebugEnabled)
                                    log.debug("Requests for EV panels was created. Result: ${toJson(result)}")
                            }
                            .toResponseDto()
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP -> throw ErrorException(ErrorType.INVALID_PMD)
                }
            }
            CommandType.VALIDATE_PERIOD -> {
                when (cm.pmd) {
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> {
                        val context = ValidatePeriodContext(country = cm.country, pmd = cm.pmd)
                        val data = toObject(ValidatePeriodRequest::class.java, cm.data).convert()
                        periodService.validatePeriod(data = data, context = context)
                            .also {
                                if (log.isDebugEnabled)
                                    log.debug("Period validation completed successfully")
                            }
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> throw ErrorException(ErrorType.INVALID_PMD)

                }
            }
            CommandType.CHECK_PERIOD -> {
                when (cm.pmd) {
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> {
                        val context = CheckPeriodContext(cpid = cm.cpidParsed(), ocid = cm.ocidParsed())
                        val data = toObject(CheckPeriodRequest::class.java, cm.data).convert()
                        periodService.checkPeriod(data = data, context = context)
                            .also { result ->
                                if (log.isDebugEnabled)
                                    log.debug("Period check completed successfully. Result: ${toJson(result)}")
                            }
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> throw ErrorException(ErrorType.INVALID_PMD)
                }
            }
            CommandType.SAVE_PERIOD -> {
                when (cm.pmd) {
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> {
                        val historyEntity = historyDao.getHistory(cm.id, cm.command.value())
                        if (historyEntity != null) Unit
                        else {
                            val ocid = if (cm.operationType == "createCNonPN" || cm.operationType == "createFE")
                                cm.ocidCnParsed()
                            else
                                cm.ocidParsed()
                            val context = SavePeriodContext(cpid = cm.cpidParsed(), ocid = ocid)
                            val data = toObject(SavePeriodRequest::class.java, cm.data).convert()
                            periodService.savePeriod(data = data, context = context)
                                .also {
                                    historyDao.saveHistory(cm.id, cm.command.value(), it)
                                    if (log.isDebugEnabled)
                                        log.debug("Period save completed successfully")
                                }
                        }
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> throw ErrorException(ErrorType.INVALID_PMD)
                }
            }

            CommandType.EXTEND_SUBMISSION_PERIOD -> {
                when (cm.pmd) {
                    ProcurementMethod.CF, ProcurementMethod.TEST_CF,
                    ProcurementMethod.GPA, ProcurementMethod.TEST_GPA,
                    ProcurementMethod.OF, ProcurementMethod.TEST_OF,
                    ProcurementMethod.RT, ProcurementMethod.TEST_RT -> {
                        val historyEntity = historyDao.getHistory(cm.id, cm.command.value())
                        if (historyEntity != null) historyEntity
                        else {
                            val context = ExtendSubmissionPeriodContext(
                                cpid = cm.cpidParsed(),
                                ocid = cm.ocidParsed(),
                                pmd = cm.pmd,
                                country = cm.country,
                                startDate = cm.startDate
                            )
                            periodService.extendSubmissionPeriod(context = context)
                                .also {
                                    historyDao.saveHistory(cm.id, cm.command.value(), it)
                                    if (log.isDebugEnabled)
                                        log.debug("Period extension completed successfully")
                                }
                        }
                    }

                    ProcurementMethod.CD, ProcurementMethod.TEST_CD,
                    ProcurementMethod.DA, ProcurementMethod.TEST_DA,
                    ProcurementMethod.DC, ProcurementMethod.TEST_DC,
                    ProcurementMethod.FA, ProcurementMethod.TEST_FA,
                    ProcurementMethod.IP, ProcurementMethod.TEST_IP,
                    ProcurementMethod.MV, ProcurementMethod.TEST_MV,
                    ProcurementMethod.NP, ProcurementMethod.TEST_NP,
                    ProcurementMethod.OP, ProcurementMethod.TEST_OP,
                    ProcurementMethod.OT, ProcurementMethod.TEST_OT,
                    ProcurementMethod.SV, ProcurementMethod.TEST_SV -> throw ErrorException(ErrorType.INVALID_PMD)
                }
            }
        }
        return ApiSuccessResponse(id = cm.id, version = cm.version, data = dataOfResponse)
            .also {
                if (log.isDebugEnabled)
                    log.debug("Response: ${toJson(it)}")
            }
    }
}
