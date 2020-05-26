package com.procurement.dossier.application.service

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsParams
import com.procurement.dossier.application.model.data.submission.state.get.GetSubmissionStateByIdsResult
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionParams
import com.procurement.dossier.application.model.data.submission.state.set.SetStateForSubmissionResult
import com.procurement.dossier.application.repository.SubmissionRepository
import com.procurement.dossier.domain.fail.Fail
import com.procurement.dossier.domain.fail.error.ValidationErrors
import com.procurement.dossier.domain.model.Cpid
import com.procurement.dossier.domain.model.Ocid
import com.procurement.dossier.domain.model.enums.SubmissionStatus
import com.procurement.dossier.domain.model.submission.SubmissionState
import com.procurement.dossier.domain.util.Result
import com.procurement.dossier.domain.util.asSuccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

internal class SubmissionServiceTest {
    companion object {
        private val CPID = Cpid.tryCreateOrNull("ocds-t1s2t3-MD-1565251033096")!!
        private val OCID = Ocid.tryCreateOrNull("ocds-b3wdp1-MD-1581509539187-EV-1581509653044")!!
        private val SUBMISSION_ID_1 = UUID.randomUUID()
        private val SUBMISSION_ID_2 = UUID.randomUUID()

        private val submissionRepository: SubmissionRepository = mock()
        private val generable: Generable = mock()
        private val submissionService: SubmissionService = SubmissionService(submissionRepository, generable)
    }

    @Nested
    inner class GetSubmissionStateByIds {

        @Test
        fun success() {
            val params = getParams()
            val storedStates = listOf(
                SubmissionState(id = params.submissionIds[0], status = SubmissionStatus.PENDING),
                SubmissionState(id = params.submissionIds[1], status = SubmissionStatus.PENDING)
            )
            whenever(
                submissionRepository.getSubmissionsStates(
                    cpid = params.cpid, ocid = params.ocid, submissionIds = params.submissionIds
                )
            )
                .thenReturn(storedStates.asSuccess())

            val actual = submissionService.getSubmissionStateByIds(params = params).get
            val expected = listOf(
                GetSubmissionStateByIdsResult(id = params.submissionIds[0], status = SubmissionStatus.PENDING),
                GetSubmissionStateByIdsResult(id = params.submissionIds[1], status = SubmissionStatus.PENDING)
            )

            assertEquals(actual, expected)
        }

        @Test
        fun notAllSubmissionsFound_fail() {
            val params = getParams()
            whenever(
                submissionRepository.getSubmissionsStates(
                    cpid = params.cpid, ocid = params.ocid, submissionIds = params.submissionIds
                )
            )
                .thenReturn(
                    listOf(SubmissionState(id = params.submissionIds[0], status = SubmissionStatus.PENDING)).asSuccess()
                )

            val actual = submissionService.getSubmissionStateByIds(params = params).error

            assertTrue(actual is ValidationErrors.SubmissionNotFoundFor)
        }

        @Test
        fun databaseIncident_fail() {
            val params = getParams()
            val expected = Result.failure(
                Fail.Incident.Database.Interaction(
                    exception = RuntimeException("")
                )
            )
            whenever(
                submissionRepository.getSubmissionsStates(
                    cpid = params.cpid, ocid = params.ocid, submissionIds = params.submissionIds
                )
            )
                .thenReturn(expected)

            val actual = submissionService.getSubmissionStateByIds(params = params)

            assertEquals(actual, expected)
        }

        private fun getParams() =
            GetSubmissionStateByIdsParams.tryCreate(
                cpid = CPID.toString(),
                ocid = OCID.toString(),
                submissionIds = listOf(
                    SUBMISSION_ID_1.toString(), SUBMISSION_ID_2.toString()
                )
            ).get
    }

    @Nested
    inner class SetStateForSubmission {
        @Test
        fun success() {
            val params = getParams()
            val submission = params.submission
            whenever(
                submissionRepository.setSubmissionStatus(
                    cpid = params.cpid, ocid = params.ocid, status = submission.status, id = submission.id
                )
            ).thenReturn(true.asSuccess())

            val actual = submissionService.setStateForSubmission(params = params).get
            val expected = SetStateForSubmissionResult(id = submission.id, status = submission.status)

            assertEquals(actual, expected)
        }

        @Test
        fun submissionNotFound_fail() {
            val params = getParams()
            val submission = params.submission
            whenever(
                submissionRepository.setSubmissionStatus(
                    cpid = params.cpid, ocid = params.ocid, status = submission.status, id = submission.id
                )
            ).thenReturn(false.asSuccess())

            val actual = submissionService.setStateForSubmission(params = params).error

            assertTrue(actual is ValidationErrors.SubmissionNotFoundFor.SetStateForSubmission)
        }

        @Test
        fun databaseIncident_fail() {
            val params = getParams()
            val submission = params.submission
            val expected = Result.failure(
                Fail.Incident.Database.Interaction(
                    exception = RuntimeException("")
                )
            )
            whenever(
                submissionRepository.setSubmissionStatus(
                    cpid = params.cpid, ocid = params.ocid, status = submission.status, id = submission.id
                )
            ).thenReturn(expected)

            val actual = submissionService.setStateForSubmission(params = params)

            assertEquals(expected, actual)
        }

        private fun getParams(): SetStateForSubmissionParams =
            SetStateForSubmissionParams.tryCreate(
                cpid = CPID.toString(),
                ocid = OCID.toString(),
                submission = SetStateForSubmissionParams.Submission.tryCreate(
                    id = SUBMISSION_ID_1.toString(),
                    status = SubmissionStatus.PENDING.key
                ).get
            ).get
    }
}