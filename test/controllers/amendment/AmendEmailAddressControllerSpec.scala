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

package controllers.amendment

import base.unit.{ControllerSpec, AmendmentControllerSpec}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import forms.contact.{
  EmailAddress,
  EmailAddressPasscode
}
import models.emailverification.EmailVerificationJourneyStatus
import models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  JOURNEY_NOT_FOUND,
  TOO_MANY_ATTEMPTS
}
import models.registration.{
  AmendRegistrationUpdateService,
  Registration
}
import services.EmailVerificationService
import views.html.contact.{
  email_address_page,
  email_address_passcode_confirmation_page,
  email_address_passcode_page,
  too_many_attempts_passcode_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class AmendEmailAddressControllerSpec
    extends ControllerSpec with AmendmentControllerSpec with TableDrivenPropertyChecks {

  private val mcc = stubMessagesControllerComponents()

  private val amendEmailPage                = mock[email_address_page]
  private val amendEmailPasscodePage        = mock[email_address_passcode_page]
  private val amendEmailConfirmationPage    = mock[email_address_passcode_confirmation_page]
  private val amendEmailTooManyAttemptsPage = mock[too_many_attempts_passcode_page]

  when(amendEmailPage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("email amendment")
  )

  when(amendEmailPasscodePage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("email passcode")
  )

  when(amendEmailConfirmationPage.apply(any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("email verification confirmation")
  )

  when(amendEmailTooManyAttemptsPage.apply()(any(), any())).thenReturn(
    HtmlFormat.raw("email verification too many attempts")
  )

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val pptReference = "XMPPT0000000123"
  private val cacheId    = amendsJourneyRequest.authenticatedRequest.cacheId

  private val populatedRegistration = {
    val basicRegistration = aRegistration()
    basicRegistration.copy(primaryContactDetails =
      basicRegistration.primaryContactDetails.copy(prospectiveEmail = Some("updatedemail@ppt.com"))
    )
  }

  override protected def beforeEach(): Unit = {
    reset(mockEmailVerificationService, mockAmendRegService)
    spyJourneyAction.setReg(populatedRegistration)
    simulateUpdateWithRegSubscriptionSuccess()
    simulateAllEmailsUnverified()
    simulateSendingEmailVerificationCodeSuccess()
  }

  private def simulateAllEmailsUnverified() =
    when(mockEmailVerificationService.isEmailVerified(any(), any())(any())).thenReturn(
      Future.successful(false)
    )

  private def simulateSendingEmailVerificationCodeSuccess() =
    when(mockEmailVerificationService.sendVerificationCode(any(), any(), any())(any())).thenReturn(
      Future.successful("email-verification-journey-id")
    )

  val controller = new AmendEmailAddressController(
    mcc,
    spyJourneyAction,
    mockAmendRegService,
    amendEmailPage,
    amendEmailPasscodePage,
    amendEmailConfirmationPage,
    amendEmailTooManyAttemptsPage,
    mockEmailVerificationService,
    inMemoryRegistrationUpdater
  )

  "Amend Email Address Controller" should {
    "show page" when {
      val showPageTestData =
        Table(("Test Name", "Display Call", "Expected Page Content"),
              ("new email address",
               (req: Request[AnyContent]) => controller.email()(req),
               "email amendment"
              ),
              ("email passcode",
               (req: Request[AnyContent]) => controller.emailVerificationCode()(req),
               "email passcode"
              ),
              ("email passcode confirmation",
               (req: Request[AnyContent]) => controller.emailVerified()(req),
               "email verification confirmation"
              ),
              ("email passcode too many verification attempts",
               (req: Request[AnyContent]) => controller.emailVerificationTooManyAttempts()(req),
               "email verification too many attempts"
              )
        )

      forAll(showPageTestData) {
        (testName: String, call: Request[AnyContent] => Future[Result], expectedContent: String) =>
          s"$testName page requested and registration populated" in {
            val resp = call(FakeRequest())

            status(resp) mustBe OK
            contentAsString(resp) mustBe expectedContent
          }

          s"$testName page requested and registration unpopulated" in {
            spyJourneyAction.setReg(populatedRegistration)

            val resp = call(FakeRequest())

            status(resp) mustBe OK
            contentAsString(resp) mustBe expectedContent
          }
      }
    }

    "redirect to the registration amendment page" when {
      "no change to email address" in {
        val resp = controller.updateEmail()(
          postRequestEncoded(form =
                               EmailAddress(populatedRegistration.primaryContactDetails.email.get),
                             sessionId = cacheId
          )
        )

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.AmendRegistrationController.displayPage().url)

        verifyNoInteractions(mockEmailVerificationService)
      }
    }

    "update the registration" when {
      "email address already verified" in {
        val previouslyVerifiedEmail = "verified@ppt.com"
        simulatePreviouslyVerifiedEmailAddress(previouslyVerifiedEmail)
        spyJourneyAction.setReg(populatedRegistration)
        inMemoryRegistrationAmendmentRepository.put(populatedRegistration)

        val resp = controller.updateEmail()(
          FakeRequest().withFormUrlEncodedBody(getTuples(EmailAddress(previouslyVerifiedEmail)):_*)
        )

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.AmendRegistrationController.displayPage().url)

        await(resp)

        val updatedReg = getUpdatedRegistrationMethod()(populatedRegistration)
        updatedReg.primaryContactDetails.email mustBe Some(previouslyVerifiedEmail)
      }
    }

    "send a verification code and prompt the user for this" when {
      "email is *not* already verified" in {

        inMemoryRegistrationAmendmentRepository.put(populatedRegistration)
        val unverifiedEmail = "unverified@ppt.com"
        simulateAllEmailsUnverified()

        val resp = controller.updateEmail()(
          postRequestEncoded(form =
                               EmailAddress(unverifiedEmail),
                             sessionId = cacheId
          )
        )

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.AmendEmailAddressController.emailVerificationCode().url
        )

        await(resp)

        val updatedReg = await(inMemoryRegistrationAmendmentRepository.get(cacheId))
        updatedReg.get.primaryContactDetails.email mustBe populatedRegistration.primaryContactDetails.email
        verifyEmailVerificationCodeSentAsExpected(unverifiedEmail)
      }
    }

    "show email verified page (do not update email yet)" when {
      "correct email verification passcode is supplied" in {
        val goodPasscode = "goodPasscode"
        simulateEmailVerificationPassword(goodPasscode, COMPLETE)

        val resp = controller.checkEmailVerificationCode()(
          postRequestEncoded(form = EmailAddressPasscode(goodPasscode), sessionId = cacheId)
        )

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.AmendEmailAddressController.emailVerified().url)
        verify(mockAmendRegService, never()).updateSubscriptionWithRegistration(any())(any(), any())
      }
    }

    "update email" when {
      "user confirms" in {
        // Email verification will be called to check this email address has actually been verified
        // and that the user has not url skipped to the end of the journey
        when(
          mockEmailVerificationService.isEmailVerified(ArgumentMatchers.eq("updatedemail@ppt.com"),
                                                       any()
          )(any())
        ).thenReturn(Future.successful(true))

        val resp = controller.confirmEmailUpdate()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.AmendRegistrationController.displayPage().url)

        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockAmendRegService).updateSubscriptionWithRegistration(any())(any(), any())
        getUpdatedRegistrationMethod().apply(populatedRegistration).primaryContactDetails.email mustBe populatedRegistration.primaryContactDetails.prospectiveEmail
      }
    }

    "inform user" when {
      "invalid new email address supplied" in {
        val invalidEmail = "xxx"

        val resp = controller.updateEmail()(
          postRequestEncoded(form = EmailAddress(invalidEmail), sessionId = cacheId)
        )

        status(resp) mustBe BAD_REQUEST
        contentAsString(resp) mustBe "email amendment"
      }

      "invalid email verification passcode supplied" in {
        val invalidPasscode = ""

        val resp = controller.checkEmailVerificationCode()(
          postRequestEncoded(form = EmailAddressPasscode(invalidPasscode), sessionId = cacheId)
        )

        status(resp) mustBe BAD_REQUEST
        contentAsString(resp) mustBe "email passcode"
      }

      "incorrect email verification passcode is supplied" in {
        val badPasscode = "badPasscode"
        simulateEmailVerificationPassword(badPasscode, INCORRECT_PASSCODE)

        val resp = controller.checkEmailVerificationCode()(
          postRequestEncoded(form = EmailAddressPasscode(badPasscode), sessionId = cacheId)
        )

        status(resp) mustBe BAD_REQUEST
        contentAsString(resp) mustBe "email passcode"
      }

      "email verification journey id cannot be found" in {
        val passcode = "passcode"
        simulateEmailVerificationPassword(passcode, JOURNEY_NOT_FOUND)

        val resp = controller.checkEmailVerificationCode()(
          postRequestEncoded(form = EmailAddressPasscode(passcode), sessionId = cacheId)
        )

        status(resp) mustBe BAD_REQUEST
        contentAsString(resp) mustBe "email passcode"
      }
    }

    "display too many attempts to verify email address page" when {
      "user exceeds max number of attempts" in {
        val passcode = "passcode"
        simulateEmailVerificationPassword(passcode, TOO_MANY_ATTEMPTS)

        val resp = controller.checkEmailVerificationCode()(
          postRequestEncoded(form = EmailAddressPasscode(passcode), sessionId = cacheId)
        )

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.AmendEmailAddressController.emailVerificationTooManyAttempts().url
        )
      }
    }
  }

  private def simulatePreviouslyVerifiedEmailAddress(email: String) =
    when(
      mockEmailVerificationService.isEmailVerified(ArgumentMatchers.eq(email), any())(any())
    ).thenReturn(Future.successful(true))


  private def verifyEmailVerificationCodeSentAsExpected(email: String) =
    verify(mockEmailVerificationService).sendVerificationCode(ArgumentMatchers.eq(email),
                                                              any(),
                                                              any()
    )(any())

  private def simulateEmailVerificationPassword(
    passcode: String,
    status: EmailVerificationJourneyStatus.Value
  ) =
    when(
      mockEmailVerificationService.checkVerificationCode(ArgumentMatchers.eq(passcode),
                                                         any(),
                                                         any()
      )(any())
    )
      .thenReturn(Future.successful(status))

}
