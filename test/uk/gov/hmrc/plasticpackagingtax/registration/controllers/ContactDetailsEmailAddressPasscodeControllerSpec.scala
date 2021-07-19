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
import org.mockito.ArgumentMatchers
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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Address, EmailAddressPasscode, FullName}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  JOURNEY_NOT_FOUND,
  JourneyStatus,
  TOO_MANY_ATTEMPTS
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.VerifyPasscodeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PrimaryContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_passcode_page
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
    dataToReturn: JourneyStatus
  ): OngoingStubbing[Future[Either[ServiceError, JourneyStatus]]] =
    when(
      mockEmailVerificationConnector.verifyPasscode(any[String], any[VerifyPasscodeRequest])(any())
    )
      .thenReturn(Future.successful(Right(dataToReturn)))

  def mockEmailVerificationVerifyPasscodeWithException(
    error: ServiceError
  ): OngoingStubbing[Future[Either[ServiceError, JourneyStatus]]] =
    when(
      mockEmailVerificationConnector.verifyPasscode(any[String], any[VerifyPasscodeRequest])(any())
    )
      .thenReturn(Future(Left(error)))

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

    forAll(Seq(continueFormAction, unKnownFormAction)) { formAction =>
      "return 200 (OK) for " + formAction._1 when {
        "user submits passcode returns complete" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscode(COMPLETE)
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
          mockEmailVerificationVerifyPasscode(INCORRECT_PASSCODE)
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))

          formAction._1 match {
            case "Continue" =>
              status(result) mustBe BAD_REQUEST
            case "Unknown" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }

        "user submits passcode returns too many attempts" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscode(TOO_MANY_ATTEMPTS)
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))

          formAction._1 match {
            case "Continue" =>
              status(result) mustBe BAD_REQUEST
            case "Unknown" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }

        "user submits passcode returns journey not found" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscode(JOURNEY_NOT_FOUND)
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))
          formAction._1 match {
            case "Continue" =>
              status(result) mustBe BAD_REQUEST
            case "Unknown" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }

          reset(mockRegistrationConnector)
        }
      }

      "throw Exception when cache fails to return email " + formAction._1 when {
        "user submits passcode" in {
          val reg = aRegistration(
            withPrimaryContactDetails(primaryContactDetails =
              PrimaryContactDetails(fullName =
                                      Some(FullName(firstName = "Jack", lastName = "Gatsby")),
                                    jobTitle = Some("Developer"),
                                    phoneNumber = Some("0203 4567 890"),
                                    address = Some(
                                      Address(addressLine1 = "2 Scala Street",
                                              addressLine2 = Some("Soho"),
                                              townOrCity = "London",
                                              postCode = "W1T 2HN"
                                      )
                                    ),
                                    journeyId = Some("journey-id")
              )
            )
          )
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))

          formAction._1 match {
            case "Continue" =>
              intercept[RegistrationException](status(result))
            case "Unknown" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }

          reset(mockRegistrationConnector)
        }
      }

      "return error when verifyPasscode throws error " + formAction._1 when {
        "user submits passcode" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate(reg)
          mockEmailVerificationVerifyPasscodeWithException(
            DownstreamServiceError("Error", RegistrationException("Error"))
          )
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK"), formAction))

          formAction._1 match {
            case "Continue" =>
              intercept[DownstreamServiceError](status(result))
            case "Unknown" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }

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

    "return prepopulated form" when {

      def pageForm: Form[EmailAddressPasscode] = {
        val form = EmailAddressPasscode.form()
        verify(page).apply(ArgumentMatchers.eq(form), ArgumentMatchers.eq(Some("test@test.com")))(
          any(),
          any()
        )
        form.fill(EmailAddressPasscode("DNCLRK"))
      }

      "data exist" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withPrimaryContactDetails(PrimaryContactDetails(email = Some("test@test.com")))
          )
        )

        await(controller.displayPage()(getRequest()))

        pageForm.get.value mustBe "DNCLRK"
      }
    }
  }
}
