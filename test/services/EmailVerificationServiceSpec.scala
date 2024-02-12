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

package services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.await
import uk.gov.hmrc.http.HeaderCarrier
import connectors.{
  DownstreamServiceError,
  EmailVerificationConnector
}
import models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  JOURNEY_NOT_FOUND,
  TOO_MANY_ATTEMPTS
}
import models.emailverification.{
  EmailStatus,
  EmailVerificationJourneyStatus,
  VerificationStatus,
  VerifyPasscodeRequest
}

import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationServiceSpec()
    extends AnyWordSpec with MockitoSugar with BeforeAndAfterEach with Matchers
    with DefaultAwaitTimeout {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val mockEmailVerificationConnector = mock[EmailVerificationConnector]

  private val emailVerificationService = new EmailVerificationService(
    mockEmailVerificationConnector
  )

  private val journeyId = "98fe3788-2d39-409c-b400-8f86ed1634ea"

  override protected def beforeEach(): Unit = {
    when(mockEmailVerificationConnector.getStatus(any())(any())).thenReturn(
      Future.successful(Right(None))
    )
    when(mockEmailVerificationConnector.create(any())(any())).thenReturn(
      Future.successful(
        Right(s"/email-verification/journey/${journeyId}?continueUrl=/ppt&origin=ppt")
      )
    )
  }

  "Email Verification Service" should {

    "identify email verification status" when {

      "emails previously verified" in {
        expectVerifiedEmails("verified@ppt.com")
        emailVerificationService.isEmailVerified("verified@ppt.com", "123").map(_ mustBe true)
      }

      "emails not verified" in {
        expectUnverifiedEmails("unverified@ppt.com")
        emailVerificationService.isEmailVerified("unverified@ppt.com", "123").map(_ mustBe false)
      }

      "emails not yet seen" in {
        emailVerificationService.isEmailVerified("xxx@xxx.com", "123").map(_ mustBe false)
      }

    }

    "send verification passcode and return journeyId" in {
      emailVerificationService.sendVerificationCode("user@ppt.com", "123", "/a-continue-url").map(
        _ mustBe journeyId
      )
    }

    "verify passcodes" when {

      "valid" in {
        simulatePasscodeVerification(COMPLETE, "XXX", "user@ppt.com", journeyId)
        emailVerificationService.checkVerificationCode("XXX", "user@ppt.com", journeyId).map(
          _ mustBe COMPLETE
        )
      }

      "invalid" in {
        simulatePasscodeVerification(INCORRECT_PASSCODE, "XXX", "user@ppt.com", journeyId)
        emailVerificationService.checkVerificationCode("XXX", "user@ppt.com", journeyId).map(
          _ mustBe INCORRECT_PASSCODE
        )
      }

      "failed due to too many attempts" in {
        simulatePasscodeVerification(TOO_MANY_ATTEMPTS, "XXX", "user@ppt.com", journeyId)
        emailVerificationService.checkVerificationCode("XXX", "user@ppt.com", journeyId).map(
          _ mustBe TOO_MANY_ATTEMPTS
        )
      }

      "journey not found" in {
        simulatePasscodeVerification(JOURNEY_NOT_FOUND, "XXX", "user@ppt.com", journeyId)
        emailVerificationService.checkVerificationCode("XXX", "user@ppt.com", journeyId).map(
          _ mustBe JOURNEY_NOT_FOUND
        )
      }
    }

    "throw exceptions" when {

      "getting email verification status fails" in {
        when(mockEmailVerificationConnector.getStatus(any())(any())).thenReturn(
          Future.successful(Left(DownstreamServiceError("BANG!", new IllegalStateException())))
        )
        intercept[Exception] {
          await(emailVerificationService.isEmailVerified("user@ppt.com", "123"))
        }
      }

      "sending verification passcode fails" in {
        when(mockEmailVerificationConnector.create(any())(any())).thenThrow(
          new IllegalStateException("BANG!")
        )
        intercept[Exception] {
          emailVerificationService.sendVerificationCode("user@ppt.com", "123", "/a-continue-url")
        }
      }

    }
  }

  private def expectVerifiedEmails(verifiedEmails: String*) =
    when(mockEmailVerificationConnector.getStatus(any())(any())).thenReturn(
      Future.successful(
        Right(
          Some(
            VerificationStatus(verifiedEmails.map(EmailStatus(_, verified = true, locked = false)))
          )
        )
      )
    )

  private def expectUnverifiedEmails(unverifiedEmails: String*) =
    when(mockEmailVerificationConnector.getStatus(any())(any())).thenReturn(
      Future.successful(
        Right(
          Some(
            VerificationStatus(
              unverifiedEmails.map(EmailStatus(_, verified = false, locked = false))
            )
          )
        )
      )
    )

  private def simulatePasscodeVerification(
    verificationStatus: EmailVerificationJourneyStatus.Value,
    passcode: String,
    email: String,
    journeyId: String
  ) =
    when(
      mockEmailVerificationConnector.verifyPasscode(journeyId,
                                                    VerifyPasscodeRequest(passcode, email)
      )
    ).thenReturn(Future.successful(Right(verificationStatus)))

}
