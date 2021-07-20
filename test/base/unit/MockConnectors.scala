/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  EmailVerificationConnector,
  IncorpIdConnector,
  PartnershipConnector,
  SoleTraderInorpIdConnector,
  SubscriptionsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationDetails,
  PartnershipCreateJourneyRequest,
  PartnershipDetails,
  SoleTraderIncorpIdCreateRequest,
  SoleTraderIncorporationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  SubscriptionCreateResponse,
  SubscriptionStatus
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockConnectors extends MockitoSugar with RegistrationBuilder with BeforeAndAfterEach {
  self: Suite =>

  val mockIncorpIdConnector: IncorpIdConnector                   = mock[IncorpIdConnector]
  val mockSoleTraderConnector: SoleTraderInorpIdConnector        = mock[SoleTraderInorpIdConnector]
  val mockPartnershipConnector: PartnershipConnector             = mock[PartnershipConnector]
  val mockSubscriptionsConnector: SubscriptionsConnector         = mock[SubscriptionsConnector]
  val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]

  def mockGetUkCompanyDetails(
    incorporationDetails: IncorporationDetails
  ): OngoingStubbing[Future[IncorporationDetails]] =
    when(mockIncorpIdConnector.getDetails(any())(any()))
      .thenReturn(Future(incorporationDetails))

  def mockGetSoleTraderDetails(
    soleTraderDetails: SoleTraderIncorporationDetails
  ): OngoingStubbing[Future[SoleTraderIncorporationDetails]] =
    when(mockSoleTraderConnector.getDetails(any())(any()))
      .thenReturn(Future(soleTraderDetails))

  def mockGetPartnershipDetails(
    partnershipDetails: PartnershipDetails
  ): OngoingStubbing[Future[PartnershipDetails]] =
    when(mockPartnershipConnector.getDetails(any())(any()))
      .thenReturn(Future(partnershipDetails))

  def mockGetSoleTraderDetailsFailure(
    ex: Exception
  ): OngoingStubbing[Future[SoleTraderIncorporationDetails]] =
    when(mockSoleTraderConnector.getDetails(any())(any()))
      .thenThrow(ex)

  def mockGetUkCompanyDetailsFailure(ex: Exception): OngoingStubbing[Future[IncorporationDetails]] =
    when(mockIncorpIdConnector.getDetails(any())(any()))
      .thenThrow(ex)

  def mockSoleTraderCreateIncorpJourneyId(redirectUrl: String): OngoingStubbing[Future[String]] =
    when(
      mockSoleTraderConnector.createJourney(any[SoleTraderIncorpIdCreateRequest])(any())
    ).thenReturn(Future.successful(redirectUrl))

  def mockCreatePartnershipGrsJourneyCreation(
    redirectUrl: String
  ): OngoingStubbing[Future[String]] =
    when(
      mockPartnershipConnector.createJourney(any[PartnershipCreateJourneyRequest])(any())
    ).thenReturn(Future.successful(redirectUrl))

  def mockSoleTraderCreateIncorpJourneyIdException(): OngoingStubbing[Future[String]] =
    when(
      mockSoleTraderConnector.createJourney(any[SoleTraderIncorpIdCreateRequest])(any())
    ).thenThrow(new RuntimeException("error"))

  def mockUkCompanyCreateIncorpJourneyId(redirectUrl: String): OngoingStubbing[Future[String]] =
    when(mockIncorpIdConnector.createJourney(any())(any()))
      .thenReturn(Future.successful(redirectUrl))

  def mockUkCompanyCreateIncorpJourneyIdException(): OngoingStubbing[Future[String]] =
    when(mockIncorpIdConnector.createJourney(any())(any())).thenThrow(new RuntimeException("error"))

  protected def mockGetSubscriptionStatusFailure(
    ex: Exception
  ): OngoingStubbing[Future[SubscriptionStatus]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any()))
      .thenThrow(ex)

  protected def mockGetSubscriptionStatus(
    subscription: SubscriptionStatus
  ): OngoingStubbing[Future[SubscriptionStatus]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockSubscriptionSubmit(
    subscription: SubscriptionCreateResponse
  ): OngoingStubbing[Future[SubscriptionCreateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(
      Future.successful(subscription)
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncorpIdConnector, mockSoleTraderConnector)
  }

}
