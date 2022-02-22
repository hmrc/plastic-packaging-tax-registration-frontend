/*
 * Copyright 2022 HM Revenue & Customs
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

import builders.RegistrationBuilder
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  EisError,
  SubscriptionCreateOrUpdateResponse,
  SubscriptionCreateOrUpdateResponseFailure,
  SubscriptionCreateOrUpdateResponseSuccess
}

import java.time.ZonedDateTime
import scala.concurrent.Future

trait MockSubscriptionConnector extends RegistrationBuilder with MockitoSugar {

  protected val mockSubscriptionConnector: SubscriptionsConnector = mock[SubscriptionsConnector]

  protected def simulateUpdateSubscriptionSuccess() =
    when(mockSubscriptionConnector.updateSubscription(any(), any())(any())).thenReturn(
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

  protected def simulateUpdateSubscriptionFailure(
    ex: RuntimeException
  ): OngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionConnector.updateSubscription(any(), any())(any())).thenReturn(
      Future.failed(ex)
    )

  protected def simulateUpdateSubscriptionFailureReturnedError()
    : OngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionConnector.updateSubscription(any(), any())(any())).thenReturn(
      Future.successful(
        SubscriptionCreateOrUpdateResponseFailure(failures =
          Seq(EisError("E1", "Big Error Number 1"))
        )
      )
    )

  protected def simulateGetSubscriptionSuccess(
    registration: Registration
  ): OngoingStubbing[Future[Registration]] =
    when(mockSubscriptionConnector.getSubscription(any())(any())).thenReturn(
      Future.successful(registration)
    )

  protected def simulateGetSubscriptionFailure(): OngoingStubbing[Future[Registration]] =
    when(mockSubscriptionConnector.getSubscription(any())(any())).thenThrow(
      new IllegalStateException("BANG!")
    )

  protected def getUpdatedRegistration(): Registration = {
    val registrationCaptor: ArgumentCaptor[Registration] =
      ArgumentCaptor.forClass(classOf[Registration])
    verify(mockSubscriptionConnector).updateSubscription(any(), registrationCaptor.capture())(any())

    registrationCaptor.getValue
  }

}
