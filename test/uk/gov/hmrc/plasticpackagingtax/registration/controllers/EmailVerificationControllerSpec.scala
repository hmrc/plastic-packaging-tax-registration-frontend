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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  DownstreamServiceError,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  CreateEmailVerificationRequest,
  EmailStatus,
  VerificationStatus
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  MetaData,
  PrimaryContactDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class EmailVerificationControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val mcc = stubMessagesControllerComponents()

  private val controller =
    new EmailVerificationController(authenticate = mockAuthAction,
                                    journeyAction = mockJourneyAction,
                                    emailVerificationConnector =
                                      mockEmailVerificationConnector,
                                    registrationConnector = mockRegistrationConnector,
                                    mcc = mcc
    )

  def mockEmailVerificationGetStatus(
    dataToReturn: VerificationStatus
  ): OngoingStubbing[Future[Either[ServiceError, Option[VerificationStatus]]]] =
    when(mockEmailVerificationConnector.getStatus(any[String])(any()))
      .thenReturn(Future.successful(Right(Some(dataToReturn))))

  "EmailVerificationController" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        mockRegistrationUpdate(aRegistration())
        mockEmailVerificationGetStatus(
          VerificationStatus(Seq(EmailStatus("test@test.com", true, false)))
        )
        val result = controller.emailVerificationCallback("credId")(getRequest())

        redirectLocation(result) mustBe Some(
          routes.ContactDetailsTelephoneNumberController.displayPage().url
        )
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        val reg: Registration = aRegistration()
        authorizedUser()
        mockRegistrationFind(reg)
        mockRegistrationUpdate(reg)
        mockEmailVerificationGetStatus(
          VerificationStatus(Seq(EmailStatus("test@test.com", true, false)))
        )
        val result = controller.emailVerificationCallback("credId")(getRequest())

        redirectLocation(result) mustBe Some(
          routes.ContactDetailsTelephoneNumberController.displayPage().url
        )
      }

      "email address is not verfied" in {
        val reg = aRegistration(
          withMetaData(metaData =
            MetaData(verifiedEmails = Seq(EmailStatus("test@test.com", false, false)))
          )
        )
        authorizedUser()
        mockRegistrationFind(reg)
        mockRegistrationUpdate(reg)
        mockEmailVerificationGetStatus(
          VerificationStatus(Seq(EmailStatus("test@test.com", false, false)))
        )
        val result = controller.emailVerificationCallback("credId")(getRequest())

        redirectLocation(result) mustBe Some(
          routes.ContactDetailsEmailAddressController.displayPage().url
        )
      }

      "email address is locked" in {
        val reg = aRegistration(
          withMetaData(metaData =
            MetaData(verifiedEmails = Seq(EmailStatus("test@test.com", false, true)))
          )
        )
        authorizedUser()
        mockRegistrationFind(reg)
        mockRegistrationUpdate(reg)
        mockEmailVerificationGetStatus(
          VerificationStatus(Seq(EmailStatus("test@test.com", false, true)))
        )
        val result = controller.emailVerificationCallback("credId")(getRequest())

        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }
    }
  }
}
