package com.procurement.dossier.infrastructure.handler.historical.submission

import com.fasterxml.jackson.databind.JsonNode
import com.procurement.dossier.application.model.data.submission.create.CreateSubmissionResult
import com.procurement.dossier.application.repository.history.HistoryRepository
import com.procurement.dossier.application.service.Logger
import com.procurement.dossier.application.service.SubmissionService
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.DataErrors
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asFailure
import com.procurement.dossier.domain.util.asSuccess
import com.procurement.dossier.domain.util.bind
import com.procurement.dossier.domain.util.extension.errorIfBlank
import com.procurement.dossier.infrastructure.converter.submission.convert
import com.procurement.dossier.infrastructure.exception.EmptyStringException
import com.procurement.dossier.infrastructure.handler.AbstractHistoricalHandler
import com.procurement.dossier.infrastructure.model.dto.bpe.Command2Type
import com.procurement.dossier.infrastructure.model.dto.bpe.tryGetParams
import com.procurement.dossier.infrastructure.model.dto.request.submission.CreateSubmissionRequest
import org.springframework.stereotype.Component

@Component
class CreateSubmissionHandler(
    logger: Logger,
    historyRepository: HistoryRepository,
    private val submissionService: SubmissionService
) :
    AbstractHistoricalHandler<Command2Type, CreateSubmissionResult>(
        logger = logger,
        historyRepository = historyRepository,
        target = CreateSubmissionResult::class.java
    ) {

    override val action: Command2Type = Command2Type.CREATE_SUBMISSION

    override fun execute(node: JsonNode): Result<CreateSubmissionResult, Fail> {
        val data = node.tryGetParams(CreateSubmissionRequest::class.java)
            .bind { it.validateTextAttributes() }
            .bind { it.convert() }
            .orForwardFail { error -> return error }

        return submissionService.createSubmission(data)
    }

    private fun CreateSubmissionRequest.validateTextAttributes(): Result<CreateSubmissionRequest, DataErrors.Validation.EmptyString> {
        try {
            submission.id.checkForBlank("submission.id")
            submission.requirementResponses?.forEachIndexed { i, requirementResponse ->
                requirementResponse.id.checkForBlank("submission.requirementResponses[$i].id")
                requirementResponse.relatedCandidate.name.checkForBlank("submission.requirementResponses[$i].relatedCandidate.name")
            }
            submission.candidates.forEachIndexed { i, candidate ->
                candidate.name.checkForBlank("submission.candidates[$i].name")
                candidate.identifier.id.checkForBlank("submission.candidates[$i].identifier.id")
                candidate.identifier.legalName.checkForBlank("submission.candidates[$i].identifier.legalName")
                candidate.identifier.scheme.checkForBlank("submission.candidates[$i].identifier.scheme")
                candidate.identifier.uri.checkForBlank("submission.candidates[$i].identifier.uri")
                candidate.additionalIdentifiers?.forEachIndexed { k, additionalIdentifier ->
                    additionalIdentifier.id.checkForBlank("submission.candidates[$i].additionalIdentifiers[$k].id")
                    additionalIdentifier.legalName.checkForBlank("submission.candidates[$i].additionalIdentifiers[$k].legalName")
                    additionalIdentifier.scheme.checkForBlank("submission.candidates[$i].additionalIdentifiers[$k].scheme")
                    additionalIdentifier.uri.checkForBlank("submission.candidates[$i].additionalIdentifiers[$k].uri")
                }
                candidate.address.streetAddress.checkForBlank("submission.candidates[$i].address.streetAddress")
                candidate.address.postalCode.checkForBlank("submission.candidates[$i].address.postalCode")
                candidate.address.addressDetails.country.description.checkForBlank("submission.candidates[$i].address.addressDetails.country.description")
                candidate.address.addressDetails.region.description.checkForBlank("submission.candidates[$i].address.addressDetails.region.description")
                candidate.address.addressDetails.locality.description.checkForBlank("submission.candidates[$i].address.addressDetails.locality.description")
                candidate.contactPoint.name.checkForBlank("submission.candidates[$i].contactPoint.name")
                candidate.contactPoint.email.checkForBlank("submission.candidates[$i].contactPoint.email")
                candidate.contactPoint.telephone.checkForBlank("submission.candidates[$i].contactPoint.telephone")
                candidate.contactPoint.faxNumber.checkForBlank("submission.candidates[$i].contactPoint.faxNumber")
                candidate.contactPoint.url.checkForBlank("submission.candidates[$i].contactPoint.url")
                candidate.persones?.forEachIndexed { k, person ->
                    person.name.checkForBlank("submission.candidates[$i].persones[$k].name")
                    person.identifier.scheme.checkForBlank("submission.candidates[$i].persones[$k].identifier.scheme")
                    person.identifier.id.checkForBlank("submission.candidates[$i].persones[$k].identifier.id")
                    person.identifier.uri.checkForBlank("submission.candidates[$i].persones[$k].identifier.uri")
                    person.businessFunctions.forEachIndexed { l, businessFunction ->
                        businessFunction.id.checkForBlank("submission.candidates[$i].persones[$k].businessFunctions[$l].id")
                        businessFunction.jobTitle.checkForBlank("submission.candidates[$i].persones[$k].businessFunctions[$l].jobTitle")
                        businessFunction.documents?.forEachIndexed { m, document ->
                            document.title.checkForBlank("submission.candidates[$i].persones[$k].businessFunctions[$l].documents[$m].title")
                            document.description.checkForBlank("submission.candidates[$i].persones[$k].businessFunctions[$l].documents[$m].description")
                        }
                    }
                }
                candidate.details.mainEconomicActivities?.forEachIndexed { k, mainEconomicActivity ->
                    mainEconomicActivity.scheme.checkForBlank("submission.candidates[$i].details.mainEconomicActivities[$k].scheme")
                    mainEconomicActivity.id.checkForBlank("submission.candidates[$i].details.mainEconomicActivities[$k].id")
                    mainEconomicActivity.description.checkForBlank("submission.candidates[$i].details.mainEconomicActivities[$k].description")
                    mainEconomicActivity.uri.checkForBlank("submission.candidates[$i].details.mainEconomicActivities[$k].uri")
                }
                candidate.details.bankAccounts?.forEachIndexed { k, bankAccount ->
                    bankAccount.description.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].description")
                    bankAccount.bankName.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].bankName")
                    bankAccount.address.streetAddress.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.streetAddress")
                    bankAccount.address.postalCode.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.postalCode")
                    bankAccount.address.addressDetails.country.id.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.country.id")
                    bankAccount.address.addressDetails.country.description.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.country.description")
                    bankAccount.address.addressDetails.country.scheme.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.country.scheme")
                    bankAccount.address.addressDetails.region.id.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.region.id")
                    bankAccount.address.addressDetails.region.description.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.region.description")
                    bankAccount.address.addressDetails.region.scheme.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.region.scheme")
                    bankAccount.address.addressDetails.locality.id.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.locality.id")
                    bankAccount.address.addressDetails.locality.description.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.locality.description")
                    bankAccount.address.addressDetails.locality.scheme.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].address.addressDetails.locality.scheme")
                    bankAccount.identifier.scheme.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].identifier.scheme")
                    bankAccount.identifier.id.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].identifier.id")
                    bankAccount.accountIdentification.id.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].accountIdentification.id")
                    bankAccount.accountIdentification.scheme.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].accountIdentification.scheme")
                    bankAccount.additionalAccountIdentifiers?.forEachIndexed { l, additionalAccountIdentifier ->
                        additionalAccountIdentifier.id.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].additionalAccountIdentifiers[$l].id")
                        additionalAccountIdentifier.scheme.checkForBlank("submission.candidates[$i].details.bankAccounts[$k].additionalAccountIdentifiers[$l].scheme")
                    }
                }
                candidate.details.legalForm?.scheme.checkForBlank("submission.candidates[$i].details.legalForm.scheme")
                candidate.details.legalForm?.id.checkForBlank("submission.candidates[$i].details.legalForm.id")
                candidate.details.legalForm?.description.checkForBlank("submission.candidates[$i].details.legalForm.description")
                candidate.details.legalForm?.uri.checkForBlank("submission.candidates[$i].details.legalForm.uri")
                submission.documents?.forEachIndexed {k, document ->
                    document.title.checkForBlank("submission.documents[$k].title")
                    document.description.checkForBlank("submission.documents[$k].description")
                }
            }
        } catch (exception: EmptyStringException) {
            return DataErrors.Validation.EmptyString(exception.attributeName).asFailure()
        }
        return this.asSuccess()
    }

    private fun String?.checkForBlank(name: String) = this.errorIfBlank { EmptyStringException(name) }
}
