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
import org.mockito.ArgumentMatchers.any
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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{EmailAddress, EmailAddressPasscode}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  EmailStatus,
  VerificationStatus,
  VerifyPasscodeRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PrimaryContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  email_address_page,
  email_address_passcode_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ContactDetailsEmailAddressPasscodeControllerSpec
    extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[email_address_passcode_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsEmailAddressPasscodeController(authenticate = mockAuthAction,
                                                     journeyAction = mockJourneyAction,
                                                     emailVerificationConnector =
                                                       mockEmailVerificationConnector,
                                                     mcc = mcc,
                                                     page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  def mockEmailVerificationVerifyPasscode(
    dataToReturn: String
  ): OngoingStubbing[Future[Either[ServiceError, String]]] =
    when(
      mockEmailVerificationConnector.verifyPasscode(any[String], any[VerifyPasscodeRequest])(any())
    )
      .thenReturn(Future.successful(Right(dataToReturn)))

  "ContactDetailsEmailAddressPasscodeController" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    forAll(Seq(continueFormAction)) { formAction =>
      "return 200 (OK) for " + formAction._1 when {
        "user submits passcode returns complete" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscode("complete")
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))
          status(result) mustBe SEE_OTHER
          formAction._1 match {
            case "Continue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsEmailAddressPasscodeConfirmationController.displayPage().url
              )
            case "Unknown" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "return 400 bad request " + formAction._1 when {
        "user submits passcode returns incorrect passcode" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscode("incorrectPasscode")
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))

          status(result) mustBe BAD_REQUEST

          reset(mockRegistrationConnector)
        }

        "user submits passcode returns too many attempts" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscode("tooManyAttempts")
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))

          status(result) mustBe BAD_REQUEST

          reset(mockRegistrationConnector)
        }

        "user submits passcode returns journey not found" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscode("journeyNotFound")
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))

          status(result) mustBe BAD_REQUEST

          reset(mockRegistrationConnector)
        }
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid passcode" in {
        authorizedUser()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddressPasscode(""))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }
  }
}
