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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  SubscriptionCreateOrUpdateResponse,
  SubscriptionCreateOrUpdateResponseFailure,
  SubscriptionCreateOrUpdateResponseSuccess,
  SubscriptionStatusResponse
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockConnectors extends MockitoSugar with RegistrationBuilder with BeforeAndAfterEach {
  self: Suite =>

  val mockUkCompanyGrsConnector: UkCompanyGrsConnector = mock[UkCompanyGrsConnector]

  val mockRegisteredSocietyGrsConnector: RegisteredSocietyGrsConnector =
    mock[RegisteredSocietyGrsConnector]

  val mockSoleTraderGrsConnector: SoleTraderGrsConnector = mock[SoleTraderGrsConnector]

  val mockPartnershipGrsConnector: PartnershipGrsConnector =
    mock[PartnershipGrsConnector]

  val mockSubscriptionsConnector: SubscriptionsConnector         = mock[SubscriptionsConnector]
  val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]

  def mockGetUkCompanyDetails(
    incorporationDetails: IncorporationDetails
  ): OngoingStubbing[Future[IncorporationDetails]] =
    when(mockUkCompanyGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(incorporationDetails))

  def mockGetRegisteredSocietyDetails(
    incorporationDetails: IncorporationDetails
  ): OngoingStubbing[Future[IncorporationDetails]] =
    when(mockRegisteredSocietyGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(incorporationDetails))

  def mockGetSoleTraderDetails(
    soleTraderDetails: SoleTraderDetails
  ): OngoingStubbing[Future[SoleTraderDetails]] =
    when(mockSoleTraderGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(soleTraderDetails))

  def mockGetPartnershipBusinessDetails(
    partnershipBusinessDetails: PartnershipBusinessDetails
  ): OngoingStubbing[Future[PartnershipBusinessDetails]] =
    when(mockPartnershipGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(partnershipBusinessDetails))

  def mockGetSoleTraderDetailsFailure(ex: Exception): OngoingStubbing[Future[SoleTraderDetails]] =
    when(mockSoleTraderGrsConnector.getDetails(any())(any(), any()))
      .thenThrow(ex)

  def mockGetPartnershipDetailsFailure(
    ex: Exception
  ): OngoingStubbing[Future[PartnershipBusinessDetails]] =
    when(mockPartnershipGrsConnector.getDetails(any())(any(), any()))
      .thenThrow(ex)

  def mockGetUkCompanyDetailsFailure(ex: Exception): OngoingStubbing[Future[IncorporationDetails]] =
    when(mockUkCompanyGrsConnector.getDetails(any())(any(), any()))
      .thenThrow(ex)

  def mockSoleTraderCreateIncorpJourneyId(redirectUrl: String): OngoingStubbing[Future[String]] =
    when(
      mockSoleTraderGrsConnector.createJourney(any[SoleTraderGrsCreateRequest])(any(), any())
    ).thenReturn(Future.successful(redirectUrl))

  def mockCreatePartnershipGrsJourneyCreation(
    redirectUrl: String
  ): OngoingStubbing[Future[String]] =
    when(
      mockPartnershipGrsConnector.createJourney(any[PartnershipGrsCreateRequest], any[String])(
        any(),
        any()
      )
    ).thenReturn(Future.successful(redirectUrl))

  def mockSoleTraderCreateIncorpJourneyIdException(): OngoingStubbing[Future[String]] =
    when(
      mockSoleTraderGrsConnector.createJourney(any[SoleTraderGrsCreateRequest])(any(), any())
    ).thenThrow(new RuntimeException("sole trader create journey error"))

  def mockUkCompanyCreateIncorpJourneyId(redirectUrl: String): OngoingStubbing[Future[String]] =
    when(mockUkCompanyGrsConnector.createJourney(any())(any(), any()))
      .thenReturn(Future.successful(redirectUrl))

  def mockRegisteredSocietyCreateIncorpJourneyId(
    redirectUrl: String
  ): OngoingStubbing[Future[String]] =
    when(mockRegisteredSocietyGrsConnector.createJourney(any())(any(), any()))
      .thenReturn(Future.successful(redirectUrl))

  def mockUkCompanyCreateIncorpJourneyIdException(): OngoingStubbing[Future[String]] =
    when(mockUkCompanyGrsConnector.createJourney(any())(any(), any())).thenThrow(
      new RuntimeException("uk company create journey error")
    )

  def mockRegisteredSocietyCreateIncorpJourneyIdException(): OngoingStubbing[Future[String]] =
    when(mockRegisteredSocietyGrsConnector.createJourney(any())(any(), any())).thenThrow(
      new RuntimeException("registered society create journey error")
    )

  protected def mockGetSubscriptionStatusFailure(
    ex: Exception
  ): OngoingStubbing[Future[SubscriptionStatusResponse]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any()))
      .thenThrow(ex)

  protected def mockGetSubscriptionStatus(
    subscription: SubscriptionStatusResponse
  ): OngoingStubbing[Future[SubscriptionStatusResponse]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockSubscriptionSubmit(
    subscription: SubscriptionCreateOrUpdateResponseSuccess
  ): OngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockSubscriptionSubmitFailure(
    ex: Exception
  ): OngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(
      Future.failed(ex)
    )

  protected def mockSubscriptionSubmitFailure(
    failureResponse: SubscriptionCreateOrUpdateResponseFailure
  ): OngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(
      Future.successful(failureResponse)
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUkCompanyGrsConnector, mockSoleTraderGrsConnector, mockRegisteredSocietyGrsConnector)
  }

}
