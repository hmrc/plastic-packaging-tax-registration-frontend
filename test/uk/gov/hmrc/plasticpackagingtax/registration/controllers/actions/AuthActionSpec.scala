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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

import base.unit.ControllerSpec
import base.{MetricsMocks, PptTestData}
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{Headers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.plasticpackagingtax.registration.config.AllowedUser
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import scala.concurrent.Future

class AuthActionSpec extends ControllerSpec with MetricsMocks {

  private val okResponseGenerator = (_: AuthenticatedRequest[_]) => Future(Results.Ok)

  private def registrationAuthAction(
    emailAllowedList: AllowedUsers = new AllowedUsers(Seq.empty)
  ): AuthAction =
    new AuthActionImpl(mockAuthConnector,
                       emailAllowedList,
                       metricsMock,
                       stubMessagesControllerComponents(),
                       appConfig
    )

  private def amendmentAuthAction(
    emailAllowedList: AllowedUsers = new AllowedUsers(Seq.empty)
  ): AuthNoEnrolmentCheckAction =
    new AuthNoEnrolmentCheckActionImpl(mockAuthConnector,
                                       emailAllowedList,
                                       metricsMock,
                                       stubMessagesControllerComponents(),
                                       appConfig
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }

  "Auth Action" should {

    "process request successfully if User Identity Data is available" in {
      val user = PptTestData.newUser("123")
      authorizedUser(user)

      await(
        registrationAuthAction().invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

    "time calls to authorisation" in {
      val user = PptTestData.newUser("123")
      authorizedUser(user)

      await(registrationAuthAction().invokeBlock(authRequest(Headers(), user), okResponseGenerator))
      metricsMock.defaultRegistry.timer(
        "ppt.registration.upstream.auth.timer"
      ).getCount should be > 1L
    }

    "process request when use email number is allowed" in {
      val allowedEmail = "amina@hmrc.co.uk"
      val user         = PptTestData.newUser("123")
      authorizedUser(user, expectedPredicate = Some(CredentialStrength(CredentialStrength.strong)))

      await(
        registrationAuthAction(
          new AllowedUsers(Seq(AllowedUser(email = allowedEmail)))
        ).invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

    "process request when use email number is allowed with different case" in {
      val allowedEmail = "Amina@HMRC.co.uk"
      val user         = PptTestData.newUser("123")
      authorizedUser(user)

      await(
        registrationAuthAction(
          new AllowedUsers(Seq(AllowedUser(email = allowedEmail)))
        ).invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

    "redirect to unauthorised page when user email is not allowed" in {
      val user = PptTestData.newUser("123")
      authorizedUser(user)

      val result =
        registrationAuthAction(
          new AllowedUsers(Seq(AllowedUser(email = "not.allowed@hmrc.co.uk")))
        ).invokeBlock(authRequest(Headers(), user), okResponseGenerator)

      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
    }

    "allow users with no existing enrolment to access registration screens by not enforcing an enrolment predicate" in {
      val allowedEmail = "amina@hmrc.co.uk"
      val user         = PptTestData.newUser("123")
      authorizedUser(user, expectedPredicate = Some(CredentialStrength(CredentialStrength.strong)))

      await(
        registrationAuthAction(
          new AllowedUsers(Seq(AllowedUser(email = allowedEmail)))
        ).invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

    "redirect to PPT account url when user already enrolled access a registration journey" in {
      when(appConfig.pptAccountUrl).thenReturn("/ppt-accounts-url")
      val user =
        PptTestData.newUser("123").copy(enrolments =
          Enrolments(
            Set(
              Enrolment(PptEnrolment.Identifier).withIdentifier(PptEnrolment.Key, "XMPPT0000000001")
            )
          )
        )
      authorizedUser(user)

      val result =
        registrationAuthAction().invokeBlock(authRequest(Headers(), user), okResponseGenerator)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/ppt-accounts-url")
    }

    "redirect to sign in when user not logged in" in {

      when(appConfig.loginUrl).thenReturn("login-url")
      when(appConfig.loginContinueUrl).thenReturn("login-continue-url")

      whenAuthFailsWith(MissingBearerToken())

      val result =
        registrationAuthAction().invokeBlock(authRequest(Headers(), PptTestData.newUser()),
                                             okResponseGenerator
        )

      redirectLocation(result) mustBe Some("login-url?continue=login-continue-url")
    }

    "redirect to unauthorised page when user not authorised" in {

      whenAuthFailsWith(InsufficientEnrolments())

      val result =
        registrationAuthAction().invokeBlock(authRequest(Headers(), PptTestData.newUser()),
                                             okResponseGenerator
        )

      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
    }

    "redirect the user to MFA Uplift page if the user has incorrect credential strength " in {
      when(appConfig.mfaUpliftUrl).thenReturn("mfa-uplift-url")
      when(appConfig.loginContinueUrl).thenReturn("login-continue-url")
      when(appConfig.serviceIdentifier).thenReturn("PPT")

      whenAuthFailsWith(IncorrectCredentialStrength())
      val result =
        registrationAuthAction().invokeBlock(authRequest(Headers(), PptTestData.newUser()),
                                             okResponseGenerator
        )

      redirectLocation(result) mustBe Some(
        "mfa-uplift-url?origin=PPT&continueUrl=login-continue-url"
      )
    }

    "only allow enrolment users to access amendment screens by enforcing an enrolment predicate" in {
      val allowedEmail = "amina@hmrc.co.uk"
      val user         = PptTestData.newUser("123")
      authorizedUser(user, expectedPredicate = Some(Enrolment(PptEnrolment.Identifier)))

      await(
        amendmentAuthAction(
          new AllowedUsers(Seq(AllowedUser(email = allowedEmail)))
        ).invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

  }
}
