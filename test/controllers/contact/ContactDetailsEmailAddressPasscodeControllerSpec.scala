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

package controllers.contact

import base.unit.ControllerSpec
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
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
import connectors.{DownstreamServiceError, ServiceError}
import controllers.contact.{routes => contactRoutes}
import controllers.{routes => pptRoutes}
import forms.contact.{Address, EmailAddress, EmailAddressPasscode}
import models.emailverification.EmailVerificationJourneyStatus.{COMPLETE, INCORRECT_PASSCODE, JOURNEY_NOT_FOUND, JourneyStatus, TOO_MANY_ATTEMPTS}
import models.registration.PrimaryContactDetails
import services.EmailVerificationService
import views.html.contact.email_address_passcode_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ContactDetailsEmailAddressPasscodeControllerSpec
    extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[email_address_passcode_page]
  private val mcc  = stubMessagesControllerComponents()

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val controller =
    new ContactDetailsEmailAddressPasscodeController(authenticate = mockAuthAction,
                                                     journeyAction = mockJourneyAction,
                                                     emailVerificationService =
                                                       mockEmailVerificationService,
                                                     registrationConnector =
                                                       mockRegistrationConnector,
                                                     mcc = mcc,
                                                     page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  def mockEmailVerificationVerifyPasscode(
    dataToReturn: JourneyStatus
  ): OngoingStubbing[Future[JourneyStatus]] =
    when(
      mockEmailVerificationService.checkVerificationCode(any[String], any[String], any[String])(
        any()
      )
    ).thenReturn(Future.successful(dataToReturn))

  def mockEmailVerificationVerifyPasscodeWithException(
    error: ServiceError
  ): OngoingStubbing[Future[JourneyStatus]] =
    when(
      mockEmailVerificationService.checkVerificationCode(any[String], any[String], any[String])(
        any()
      )
    ).thenReturn(Future.failed(error))

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

    "display page for group member" in {
      authorizedUser()
      mockRegistrationFind(aRegistration(
        withGroupDetail(Some(groupDetailsWithMembers))
      ))
      await(controller.displayPage()(getRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Option[String]])
      verify(page).apply(any(), any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe Some("primaryContactDetails.group.sectionHeader")
    }

    "display page for single member" in {
      authorizedUser()
      mockRegistrationFind(aRegistration())
      await(controller.displayPage()(getRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Option[String]])
      verify(page).apply(any(), any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe Some("primaryContactDetails.sectionHeader")
    }

      "return 200 (OK)" when {
        "user submits passcode returns complete" in {
          val email = "test2352356523332453@test.com"
          val reg =
            aRegistration(withPrimaryContactDetails(PrimaryContactDetails(email = Some(email))))
          reg.metaData.emailVerified(email) mustBe false

          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockEmailVerificationVerifyPasscode(COMPLETE)
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK")))
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            contactRoutes.ContactDetailsEmailAddressPasscodeConfirmationController.displayPage().url
          )
          modifiedRegistration.metaData.emailVerified(email) mustBe true

          reset(mockRegistrationConnector)
        }
      }

      "return 400 bad request" when {
        "user submits passcode returns incorrect passcode" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockEmailVerificationVerifyPasscode(INCORRECT_PASSCODE)
          val result = controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK")))


          status(result) mustBe BAD_REQUEST

          reset(mockRegistrationConnector)
        }

        "user submits passcode returns too many attempts" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockEmailVerificationVerifyPasscode(TOO_MANY_ATTEMPTS)

          val result = controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK")))

          status(result) mustBe SEE_OTHER

          reset(mockRegistrationConnector)
        }

        "user submits passcode returns journey not found" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockEmailVerificationVerifyPasscode(JOURNEY_NOT_FOUND)
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK")))

          status(result) mustBe BAD_REQUEST

          reset(mockRegistrationConnector)
        }
      }

      "throw Exception when cache fails to return email " when {
        "user submits passcode" in {
          val reg = aRegistration(
            withPrimaryContactDetails(primaryContactDetails =
              PrimaryContactDetails(name =
                                      Some("Jack Gatsby"),
                                    jobTitle = Some("Developer"),
                                    phoneNumber = Some("0203 4567 890"),
                                    address = Some(
                                      Address(addressLine1 = "2 Scala Street",
                                              addressLine2 = Some("Soho"),
                                              addressLine3 = None,
                                              townOrCity = "London",
                                              maybePostcode = Some("W1T 2HN"),
                                              countryCode = "GB"
                                      )
                                    ),
                                    journeyId = Some("journey-id")
              )
            )
          )
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          val result =
            controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK")))


          intercept[RegistrationException](status(result))


          reset(mockRegistrationConnector)
        }
      }

      "return error when verifyPasscode throws error " when {
        "user submits passcode" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockEmailVerificationVerifyPasscodeWithException(
            DownstreamServiceError("Error", RegistrationException("Error"))
          )
          val result = controller.submit()(postRequestEncoded(EmailAddressPasscode("DNCLRK")))

          intercept[DownstreamServiceError](status(result))
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
        verify(page).apply(ArgumentMatchers.eq(form),
                           ArgumentMatchers.eq(Some("test@test.com")),
                           ArgumentMatchers.eq(
                             routes.ContactDetailsEmailAddressPasscodeController.submit()
                           ),
                           any()
        )(any(), any())
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
