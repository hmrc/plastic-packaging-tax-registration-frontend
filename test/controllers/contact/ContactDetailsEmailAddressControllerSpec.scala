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
import connectors.{DownstreamServiceError, ServiceError}
import controllers.{routes => pptRoutes}
import forms.contact.{Address, EmailAddress}
import models.emailverification.{EmailStatus, VerificationStatus}
import models.registration.{MetaData, PrimaryContactDetails}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.EmailVerificationService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.contact.email_address_page

import scala.concurrent.Future

class ContactDetailsEmailAddressControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[email_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val controller =
    new ContactDetailsEmailAddressController(journeyAction = spyJourneyAction,
                                             emailVerificationService =
                                               mockEmailVerificationService,
                                             registrationConnector = mockRegistrationConnector,
                                             mcc = mcc,
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
    when(mockEmailVerificationService.getStatus(any[String])(any())).thenReturn(
      Future(Right(dataToReturn))
    )

  def mockEmailVerificationGetStatusWithException(
    error: ServiceError
  ): OngoingStubbing[Future[Either[ServiceError, Option[VerificationStatus]]]] =
    when(mockEmailVerificationService.getStatus(any[String])(any())).thenReturn(Future(Left(error)))

  def mockEmailVerificationCreate(dataToReturn: String): OngoingStubbing[Future[String]] =
    when(mockEmailVerificationService.sendVerificationCode(any(), any(), any())(any())).thenReturn(
      Future.successful(dataToReturn)
    )

  def mockEmailVerificationCreateWithException(
    error: ServiceError
  ): OngoingStubbing[Future[String]] =
    when(mockEmailVerificationService.sendVerificationCode(any(), any(), any())(any())).thenReturn(
      Future.failed(error)
    )

  "ContactDetailsEmailAddressController" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        spyJourneyAction.setReg(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {

        spyJourneyAction.setReg(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

      "return 303 (OK)"  when {
        "user submits an email address" in {
          val reg = aRegistration()

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(Seq(EmailStatus("test@test.com", verified = true, locked = false)))
            )
          )

          val result = controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.primaryContactDetails.email mustBe Some("test@test.com")

          redirectLocation(result) mustBe Some(
            routes.ContactDetailsTelephoneNumberController.displayPage().url
          )

          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for no status response" when {
        "user submits an email address" in {
          val reg = aRegistration()

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
          mockEmailVerificationGetStatus(None)

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.primaryContactDetails.email mustBe Some("test@test.com")

          redirectLocation(result) mustBe Some(
            routes.ContactDetailsTelephoneNumberController.displayPage().url
          )

          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for get status throw error" when {
        "user submits an email address" in {
          val reg = aRegistration()

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
          mockEmailVerificationGetStatusWithException(
            DownstreamServiceError("Failed to get status", new Exception())
          )

          intercept[DownstreamServiceError] {
            await(
              controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))
            )
          }
          reset(mockRegistrationConnector)
          reset(mockEmailVerificationService)
        }
      }

      "return 303 (OK) for not verified email address " when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails = Seq(EmailStatus("test@test.com", false, false)))
            )
          )

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus("test1@test.com", verified = false, locked = false))
              )
            )
          )
          mockEmailVerificationCreate("/email-verification/journey/234234234-23423/passcode")

          val result = controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            routes.ContactDetailsEmailAddressPasscodeController.displayPage().url
          )

          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for locked out " when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails =
                Seq(EmailStatus(emailAddress = "test@test.com", false, true))
              )
            )
          )

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus(emailAddress = "test1@test.com", verified = false, locked = false))
              )
            )
          )

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK)" when {
        "user submits an email address with email-verification disabled " in {

          spyJourneyAction.setReg(aRegistration())
          mockRegistrationUpdate()

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))
          status(result) mustBe SEE_OTHER


          redirectLocation(result) mustBe Some(
            routes.ContactDetailsTelephoneNumberController.displayPage().url
          )

          reset(mockRegistrationConnector)
        }
      }

      "return 303 (OK) for create verification email throw error " when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails =
                Seq(EmailStatus(emailAddress = "test@test.com", verified = false, locked = false))
              )
            )
          )

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
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


            intercept[DownstreamServiceError] {
              await(
                controller.submit()(
                  postRequestEncoded(EmailAddress("test2@test.com"))
                )
              )
            }

          reset(mockRegistrationConnector)
        }
      }

      "throw exception when cache registration with no email " when {
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

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus(emailAddress = "test2@test.com", verified = false, locked = false))
              )
            )
          )


          intercept[DownstreamServiceError] {
            await(
              controller.submit()(
                postRequestEncoded(EmailAddress("test2@test.com"))
              )
            )
          }

          reset(mockRegistrationConnector)
        }
      }

      "return to Registration page when no credentials " when {
        "user submits an email address" in {
          val reg = aRegistration(
            withMetaData(metaData =
              MetaData(verifiedEmails =
                Seq(EmailStatus(emailAddress = "test@test.com", verified = false, locked = false))
              )
            )
          )

          spyJourneyAction.setReg(reg)
          mockRegistrationUpdate()
          mockEmailVerificationGetStatus(
            Some(
              VerificationStatus(
                Seq(EmailStatus(emailAddress = "test2@test.com", verified = false, locked = false))
              )
            )
          )
          intercept[DownstreamServiceError] {
            await(
              controller.submit()(postRequestEncoded(EmailAddress("test2@test.com")))
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

        spyJourneyAction.setReg(
          aRegistration(
            withPrimaryContactDetails(PrimaryContactDetails(email = Some("test@test.com")))
          )
        )

        await(controller.displayPage()(getRequest()))

        pageForm.get.value mustBe "test@test.com"
      }
    }

    "return page for a group" in {

      spyJourneyAction.setReg(
        aRegistration(
          withPrimaryContactDetails(PrimaryContactDetails(email = Some("test@test.com"))),
          withGroupDetail(Some(groupDetailsWithMembers))
        )
      )

      await(controller.displayPage()(getRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(page).apply(any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe true
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid email address" in {

        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user submits form and the registration update fails" in {

        mockRegistrationUpdateFailure()


        intercept[DownstreamServiceError](status(
          controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))
        ))
      }

      "user submits form and a registration update runtime exception occurs" in {

        mockRegistrationException()

        intercept[RuntimeException](status(controller.submit()(postRequestEncoded(EmailAddress("test@test.com")))))
      }

  }

}
