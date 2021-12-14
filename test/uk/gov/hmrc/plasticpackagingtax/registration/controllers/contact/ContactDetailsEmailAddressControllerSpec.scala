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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.{Address, EmailAddress}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  CreateEmailVerificationRequest,
  EmailStatus,
  VerificationStatus
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  MetaData,
  PrimaryContactDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.email_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ContactDetailsEmailAddressControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[email_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsEmailAddressController(authenticate = mockAuthAction,
                                             journeyAction = mockJourneyAction,
                                             emailVerificationConnector =
                                               mockEmailVerificationConnector,
                                             registrationConnector = mockRegistrationConnector,
                                             mcc = mcc,
                                             config,
                                             page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  def mockEmailVerificationGetStatus(
    dataToReturn: Option[VerificationStatus]
  ): OngoingStubbing[Future[Either[ServiceError, Option[VerificationStatus]]]] =
    when(mockEmailVerificationConnector.getStatus(any[String])(any())).thenReturn(
      Future(Right(dataToReturn))
    )

  def mockEmailVerificationGetStatusWithException(
    error: ServiceError
  ): OngoingStubbing[Future[Either[ServiceError, Option[VerificationStatus]]]] =
    when(mockEmailVerificationConnector.getStatus(any[String])(any())).thenReturn(
      Future(Left(error))
    )

  def mockAppConfigEmailVerificationEnabled(enabled: Boolean): OngoingStubbing[Boolean] =
    when(config.emailVerificationEnabled)
      .thenReturn(enabled)

  def mockEmailVerificationCreate(
    dataToReturn: String
  ): OngoingStubbing[Future[Either[ServiceError, String]]] =
    when(
      mockEmailVerificationConnector.create(any[CreateEmailVerificationRequest])(any())
    ).thenReturn(Future.successful(Right(dataToReturn)))

  def mockEmailVerificationCreateWithException(
    error: ServiceError
  ): OngoingStubbing[Future[Either[ServiceError, String]]] =
    when(
      mockEmailVerificationConnector.create(any[CreateEmailVerificationRequest])(any())
    ).thenReturn(Future(Left(error)))

  "ContactDetailsEmailAddressController" should {

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

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(Seq(EmailStatus("test@test.com", verified = true, locked = false)))
            )
          )

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.primaryContactDetails.email mustBe Some("test@test.com")
          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsTelephoneNumberController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for no status response" + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatus(None)

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.primaryContactDetails.email mustBe Some("test@test.com")
          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsTelephoneNumberController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for get status throw error" + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration()
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatusWithException(
            DownstreamServiceError("Failed to get status", new Exception())
          )

          intercept[DownstreamServiceError] {
            await(
              controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))
            )
          }
          reset(mockRegistrationConnector)
          reset(mockEmailVerificationConnector)
        }
      }

      "return 303 (OK) for not verified email address " + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails = Seq(EmailStatus("test@test.com", false, false)))
            )
          )
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus("test1@test.com", verified = false, locked = false))
              )
            )
          )
          mockEmailVerificationCreate("/email-verification/journey/234234234-23423/passcode")

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))

          status(result) mustBe SEE_OTHER
          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsEmailAddressPasscodeController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for locked out " + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails =
                Seq(EmailStatus(emailAddress = "test@test.com", false, true))
              )
            )
          )
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus(emailAddress = "test1@test.com", verified = false, locked = false))
              )
            )
          )

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))

          status(result) mustBe SEE_OTHER
          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK)  " + formAction._1 when {
        "user submits an email address with email-verification disabled " in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(false)

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))
          status(result) mustBe SEE_OTHER
          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsTelephoneNumberController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for create verification email throw error " + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails =
                Seq(EmailStatus(emailAddress = "test@test.com", verified = false, locked = false))
              )
            )
          )
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus(emailAddress = "test2@test.com", verified = false, locked = false))
              )
            )
          )
          mockEmailVerificationCreateWithException(
            DownstreamServiceError("Failed to get status", new Exception())
          )

          formAction._1 match {
            case "SaveAndContinue" =>
              intercept[DownstreamServiceError] {
                await(
                  controller.submit()(
                    postRequestEncoded(EmailAddress("test2@test.com"), formAction)
                  )
                )
              }
            case "SaveAndComeBackLater" =>
              val result =
                controller.submit()(postRequestEncoded(EmailAddress("test2@test.com"), formAction))
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "throw exception when cache registration with no email " + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails =
                Seq(EmailStatus(emailAddress = "test@test.com", verified = false, locked = false))
              )
            ),
            withPrimaryContactDetails(primaryContactDetails =
              PrimaryContactDetails(name =
                                      Some("Jack Gatsby"),
                                    jobTitle = Some("Developer"),
                                    phoneNumber = Some("0203 4567 890"),
                                    address = Some(
                                      Address(addressLine1 = "2 Scala Street",
                                              addressLine2 = Some("Soho"),
                                              townOrCity = "London",
                                              postCode = Some("W1T 2HN")
                                      )
                                    ),
                                    journeyId = Some("journey-id")
              )
            )
          )
          authorizedUser()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus(emailAddress = "test2@test.com", verified = false, locked = false))
              )
            )
          )

          formAction._1 match {
            case "SaveAndContinue" =>
              intercept[DownstreamServiceError] {
                await(
                  controller.submit()(
                    postRequestEncoded(EmailAddress("test2@test.com"), formAction)
                  )
                )
              }
            case "SaveAndComeBackLater" =>
              val result =
                controller.submit()(postRequestEncoded(EmailAddress("test2@test.com"), formAction))
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }

      "return to Registration page when no credentials " + formAction._1 when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails =
                Seq(EmailStatus(emailAddress = "test@test.com", verified = false, locked = false))
              )
            )
          )
          authorizedUserWithNoCredentials()
          mockRegistrationFind(reg)
          mockRegistrationUpdate()
          mockAppConfigEmailVerificationEnabled(true)
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus(emailAddress = "test2@test.com", verified = false, locked = false))
              )
            )
          )
          intercept[DownstreamServiceError] {
            await(
              controller.submit()(postRequestEncoded(EmailAddress("test2@test.com"), formAction))
            )
          }
          reset(mockRegistrationConnector)
        }
      }
    }

    "return prepopulated form" when {

      def pageForm: Form[EmailAddress] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[EmailAddress]])
        verify(page).apply(captor.capture(), any(), any())(any(), any())
        captor.getValue
      }

      "data exist" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withPrimaryContactDetails(PrimaryContactDetails(email = Some("test@test.com")))
          )
        )

        await(controller.displayPage()(getRequest()))

        pageForm.get.value mustBe "test@test.com"
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid email address" in {
        authorizedUser()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationUpdateFailure()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[DownstreamServiceError](status(result))
      }

      "user submits form and a registration update runtime exception occurs" in {
        authorizedUser()
        mockRegistrationException()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[RuntimeException](status(result))
      }
    }
  }

}
