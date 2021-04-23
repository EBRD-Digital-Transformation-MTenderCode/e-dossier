package com.procurement.dossier.infrastructure.handler.validate.submission

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.util.ValidationResult
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.handler.AbstractValidationHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.CheckPresenceCandidateInOneSubmissionRequest
import org.springframework.stereotype.Component

@Component
class CheckPresenceCandidateInOneSubmissionHandler(logger: Logger, private val submissionService: SubmissionService) :
    AbstractValidationHandler<Command2Type>(logger) {

    override val action: Command2Type = Command2Type.CHECK_PRESENCE_CANDIDATE_IN_ONE_SUBMISSION

    override fun execute(node: JsonNode): ValidationResult<Fail> {
        val params = node.tryGetParams(CheckPresenceCandidateInOneSubmissionRequest::class.java)
            .doReturn { error -> return ValidationResult.error(error) }
            .convert()

        return submissionService.checkPresenceCandidateInOneSubmission(params)
    }
}