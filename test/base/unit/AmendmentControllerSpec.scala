/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package base.unit

import models.registration.{AmendRegistrationUpdateService, Registration, RegistrationUpdater}
import models.subscriptions.{EisError, SubscriptionCreateOrUpdateResponse, SubscriptionCreateOrUpdateResponseFailure, SubscriptionCreateOrUpdateResponseSuccess}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{verify, when}
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import services.AmendRegistrationService

import java.time.ZonedDateTime
import scala.concurrent.Future

trait AmendmentControllerSpec extends MockitoSugar with MockRegistrationAmendmentRepository{

  val inMemoryRegistrationUpdater = new AmendRegistrationUpdateService(inMemoryRegistrationAmendmentRepository)

  val mockAmendRegService: AmendRegistrationService = mock[AmendRegistrationService]
  val mockRegistrationUpdater: RegistrationUpdater = mock[RegistrationUpdater]

  protected def simulateUpdateWithRegSubscriptionSuccess(): ScalaOngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockAmendRegService.updateSubscriptionWithRegistration(any())(any(), any())).thenReturn(
      Future.successful(
        SubscriptionCreateOrUpdateResponseSuccess(pptReference = "XMPPT0000000123",
          processingDate = ZonedDateTime.now(),
          formBundleNumber = "ABC123",
          nrsNotifiedSuccessfully = true,
          nrsSubmissionId = Some("NRS123"),
          nrsFailureReason = None,
          enrolmentInitiatedSuccessfully = Some(true)
        )
      )
    )

  protected def simulateUpdateWithRegSubscriptionFailure(
                                                   ex: RuntimeException
                                                 ): ScalaOngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockAmendRegService.updateSubscriptionWithRegistration(any())(any(), any())).thenReturn(
      Future.failed(ex)
    )

  protected def simulateUpdateSubscriptionWithRegFailureReturnedError()
  : ScalaOngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockAmendRegService.updateSubscriptionWithRegistration(any())(any(), any())).thenReturn(
      Future.successful(
        SubscriptionCreateOrUpdateResponseFailure(failures =
          Seq(EisError("E1", "Big Error Number 1"))
        )
      )
    )

  protected def getUpdatedRegistrationMethod(): Registration => Registration = {
    val registrationCaptor: ArgumentCaptor[Registration => Registration] =
      ArgumentCaptor.forClass(classOf[Registration => Registration])
    verify(mockAmendRegService).updateSubscriptionWithRegistration(registrationCaptor.capture())(any(), any())

    registrationCaptor.getValue
  }

}
