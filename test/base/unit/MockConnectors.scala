/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors._
import connectors.grs._
import models.genericregistration._
import models.subscriptions.{SubscriptionCreateOrUpdateResponse, SubscriptionCreateOrUpdateResponseFailure, SubscriptionCreateOrUpdateResponseSuccess, SubscriptionStatusResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar

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

  val mockSubscriptionsConnector: SubscriptionsConnector = mock[SubscriptionsConnector]

  def mockGetUkCompanyDetails(incorporationDetails: IncorporationDetails): ScalaOngoingStubbing[Future[IncorporationDetails]] =
    when(mockUkCompanyGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(incorporationDetails))

  def mockGetRegisteredSocietyDetails(incorporationDetails: IncorporationDetails): ScalaOngoingStubbing[Future[IncorporationDetails]] =
    when(mockRegisteredSocietyGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(incorporationDetails))

  def mockGetSoleTraderDetails(soleTraderDetails: SoleTraderDetails): ScalaOngoingStubbing[Future[SoleTraderDetails]] =
    when(mockSoleTraderGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(soleTraderDetails))

  def mockGetPartnershipBusinessDetails(partnershipBusinessDetails: PartnershipBusinessDetails): ScalaOngoingStubbing[Future[PartnershipBusinessDetails]] =
    when(mockPartnershipGrsConnector.getDetails(any())(any(), any()))
      .thenReturn(Future(partnershipBusinessDetails))

  def mockGetSoleTraderDetailsFailure(ex: Exception): ScalaOngoingStubbing[Future[SoleTraderDetails]] =
    when(mockSoleTraderGrsConnector.getDetails(any())(any(), any()))
      .thenThrow(ex)

  def mockGetPartnershipDetailsFailure(ex: Exception): ScalaOngoingStubbing[Future[PartnershipBusinessDetails]] =
    when(mockPartnershipGrsConnector.getDetails(any())(any(), any()))
      .thenThrow(ex)

  def mockGetUkCompanyDetailsFailure(ex: Exception): ScalaOngoingStubbing[Future[IncorporationDetails]] =
    when(mockUkCompanyGrsConnector.getDetails(any())(any(), any()))
      .thenThrow(ex)

  def mockSoleTraderCreateIncorpJourneyId(redirectUrl: String): ScalaOngoingStubbing[Future[String]] =
    when(mockSoleTraderGrsConnector.createJourney(any[SoleTraderGrsCreateRequest])(any(), any())).thenReturn(Future.successful(redirectUrl))

  def mockCreatePartnershipGrsJourneyCreation(redirectUrl: String): ScalaOngoingStubbing[Future[String]] =
    when(mockPartnershipGrsConnector.createJourney(any[PartnershipGrsCreateRequest], any[String])(any(), any())).thenReturn(Future.successful(redirectUrl))

  def mockCreateSoleTraderPartnershipGrsJourneyCreation(redirectUrl: String): ScalaOngoingStubbing[Future[String]] =
    when(mockSoleTraderGrsConnector.createJourney(any[SoleTraderGrsCreateRequest], any[String])(any(), any())).thenReturn(Future.successful(redirectUrl))

  def mockCreateUkCompanyPartnershipGrsJourneyCreation(redirectUrl: String): ScalaOngoingStubbing[Future[String]] =
    when(mockUkCompanyGrsConnector.createJourney(any[IncorpEntityGrsCreateRequest], any[String])(any(), any())).thenReturn(Future.successful(redirectUrl))

  def mockCreateRegisteredSocietyPartnershipGrsJourneyCreation(redirectUrl: String): ScalaOngoingStubbing[Future[String]] =
    when(mockRegisteredSocietyGrsConnector.createJourney(any[IncorpEntityGrsCreateRequest], any[String])(any(), any())).thenReturn(Future.successful(redirectUrl))

  def lastPartnershipGrsJourneyCreation(): (PartnershipGrsCreateRequest, String) = {
    val partnershipGrsCreateRequestCaptor: ArgumentCaptor[PartnershipGrsCreateRequest] =
      ArgumentCaptor.forClass(classOf[PartnershipGrsCreateRequest])
    val partnershipGrsUrlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    verify(mockPartnershipGrsConnector).createJourney(partnershipGrsCreateRequestCaptor.capture(), partnershipGrsUrlCaptor.capture())(any(), any())

    (partnershipGrsCreateRequestCaptor.getValue, partnershipGrsUrlCaptor.getValue)
  }

  def mockSoleTraderCreateIncorpJourneyIdException(): ScalaOngoingStubbing[Future[String]] =
    when(mockSoleTraderGrsConnector.createJourney(any[SoleTraderGrsCreateRequest])(any(), any())).thenThrow(new RuntimeException("sole trader create journey error"))

  def mockUkCompanyCreateIncorpJourneyId(redirectUrl: String): ScalaOngoingStubbing[Future[String]] =
    when(mockUkCompanyGrsConnector.createJourney(any())(any(), any()))
      .thenReturn(Future.successful(redirectUrl))

  def mockRegisteredSocietyCreateIncorpJourneyId(redirectUrl: String): ScalaOngoingStubbing[Future[String]] =
    when(mockRegisteredSocietyGrsConnector.createJourney(any())(any(), any()))
      .thenReturn(Future.successful(redirectUrl))

  def mockUkCompanyCreateIncorpJourneyIdException(): ScalaOngoingStubbing[Future[String]] =
    when(mockUkCompanyGrsConnector.createJourney(any())(any(), any())).thenThrow(new RuntimeException("uk company create journey error"))

  def mockRegisteredSocietyCreateIncorpJourneyIdException(): ScalaOngoingStubbing[Future[String]] =
    when(mockRegisteredSocietyGrsConnector.createJourney(any())(any(), any())).thenThrow(new RuntimeException("registered society create journey error"))

  protected def mockGetSubscriptionStatusFailure(ex: Exception): ScalaOngoingStubbing[Future[SubscriptionStatusResponse]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any()))
      .thenThrow(ex)

  protected def mockGetSubscriptionStatus(subscription: SubscriptionStatusResponse): ScalaOngoingStubbing[Future[SubscriptionStatusResponse]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any())).thenReturn(Future.successful(subscription))

  protected def mockSubscriptionSubmit(subscription: SubscriptionCreateOrUpdateResponseSuccess): ScalaOngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(Future.successful(subscription))

  protected def mockSubscriptionSubmitFailure(ex: Exception): ScalaOngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(Future.failed(ex))

  protected def mockSubscriptionSubmitFailure(failureResponse: SubscriptionCreateOrUpdateResponseFailure): ScalaOngoingStubbing[Future[SubscriptionCreateOrUpdateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(Future.successful(failureResponse))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUkCompanyGrsConnector, mockSoleTraderGrsConnector, mockRegisteredSocietyGrsConnector, mockPartnershipGrsConnector)
  }

}
