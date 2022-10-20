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
import play.api.mvc.request.RequestTarget
import play.api.mvc.{Headers, Request, RequestHeader, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.unauthorised.{routes => unauthorisedRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import scala.concurrent.Future

class NotEnrolledAuthActionSpec extends ControllerSpec with MetricsMocks {

  private val okResponseGenerator = (_: AuthenticatedRequest[_]) => Future(Results.Ok)

  private val expectedAcceptableCredentialsPredicate =
    AffinityGroup.Agent.or(CredentialStrength(CredentialStrength.strong))

  private def registrationAuthAction: NotEnrolledAuthAction =
    new NotEnrolledAuthActionImpl(mockAuthConnector,
                       metricsMock,
                       stubMessagesControllerComponents(),
                       appConfig
    )

  private def amendmentAuthAction: EnrolledAuthAction =
    new EnrolledAuthActionImpl(mockAuthConnector,
                                       metricsMock,
                                       stubMessagesControllerComponents(),
                                       appConfig
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
    reset(appConfig)
  }

  "Auth Action" should {

    "process request successfully if User Identity Data is available" in {
      val user = PptTestData.newUser("123")
      authorizedUser(user)

      await(
        registrationAuthAction.invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

    "time calls to authorisation" in {
      val user = PptTestData.newUser("123")
      authorizedUser(user)

      await(registrationAuthAction.invokeBlock(authRequest(Headers(), user), okResponseGenerator))
      metricsMock.defaultRegistry.timer(
        "ppt.registration.upstream.auth.timer"
      ).getCount should be > 1L
    }

    "allow users with no existing enrolment to access registration screens by not enforcing an enrolment predicate" in {
      val user = PptTestData.newUser("123")
      authorizedUser(user,
                     expectedPredicate = Some(User.and(expectedAcceptableCredentialsPredicate))
      )

      await(
        registrationAuthAction.invokeBlock(authRequest(Headers(), user), okResponseGenerator)
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
        registrationAuthAction.invokeBlock(authRequest(Headers(), user), okResponseGenerator)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/ppt-accounts-url")
    }

    "redirect to sign in when user not logged in" in {

      when(appConfig.loginUrl).thenReturn("login-url")

      whenAuthFailsWith(MissingBearerToken())

      val request = authRequest(
        FakeRequest()
          .withHeaders(Headers())
          .withTarget(RequestTarget("/anyURi", "continueUrl", Map("key" -> Seq("value")))),
          PptTestData.newUser("123")
        )
      val result = registrationAuthAction.invokeBlock(request, okResponseGenerator)

      redirectLocation(result) mustBe Some("login-url?continue=continueUrl")
    }

    "redirect to returns accounts to use its not enrolled page when user is not enrolled" in {
      when(appConfig.pptAccountUrl).thenReturn("/ppt-accounts-url")
      whenAuthFailsWith(InsufficientEnrolments())

      val result =
        registrationAuthAction.invokeBlock(authRequest(Headers(), PptTestData.newUser()),
                                           okResponseGenerator
        )

      redirectLocation(result) mustBe Some("/ppt-accounts-url")
    }

    "redirect organisations with Assistant credential role to the assistant unauthorised page" in {
      whenAuthFailsWith(UnsupportedCredentialRole())

      val result = registrationAuthAction.invokeBlock(authRequest(Headers(), PptTestData.newUser()), okResponseGenerator)

      redirectLocation(result) mustBe Some(unauthorisedRoutes.UnauthorisedController.showAssistantUnauthorised().url)
    }

    "redirect agents to the agent unauthorised page" in {
      val agent = PptTestData.newAgent("456")
      authorizedUser(agent)
      val result = registrationAuthAction.invokeBlock(authRequest(Headers(), PptTestData.newUser()), okResponseGenerator)

      redirectLocation(result) mustBe Some(unauthorisedRoutes.UnauthorisedController.showAgentUnauthorised().url)
    }

    "redirect to the unauthorised page when the user is not authorised" in {
      whenAuthFailsWith(InternalError("Some general auth exception"))

      val result = registrationAuthAction.invokeBlock(authRequest(Headers(), PptTestData.newUser()), okResponseGenerator)

      redirectLocation(result) mustBe Some(unauthorisedRoutes.UnauthorisedController.showGenericUnauthorised().url)
    }

    "redirect the user to MFA Uplift page if the user has incorrect credential strength " in {
      when(appConfig.mfaUpliftUrl).thenReturn("mfa-uplift-url")
      when(appConfig.serviceIdentifier).thenReturn("PPT")

      whenAuthFailsWith(IncorrectCredentialStrength())

      val request = authRequest(
        FakeRequest()
          .withHeaders(Headers())
          .withTarget(RequestTarget("/anyURi", "continueUrl", Map("key" -> Seq("value")))),
        PptTestData.newUser("123")
      )
      val result = registrationAuthAction.invokeBlock(request, okResponseGenerator)

      redirectLocation(result) mustBe Some(
        "mfa-uplift-url?origin=PPT&continueUrl=continueUrl"
      )
    }

    "only allow enrolment users to access amendment screens by enforcing an enrolment predicate" in {
      val user = PptTestData.newUser("123")
      authorizedUser(
        user,
        expectedPredicate =
          Some(
            User.and(Enrolment(PptEnrolment.Identifier).and(expectedAcceptableCredentialsPredicate))
          )
      )

      await(
        amendmentAuthAction.invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

    "agents accessing an amendment screen without a selected client should be redirected back to plastics account" in {
      // The plastics accounts UI will take the required actions; we will not duplicate them here
      when(appConfig.pptAccountUrl).thenReturn("/ppt-accounts-url")

      val agent = PptTestData.newAgent("456")
      authorizedUser(
        agent,
        expectedPredicate =
          Some(
            User.and(Enrolment(PptEnrolment.Identifier).and(expectedAcceptableCredentialsPredicate))
          )
      )

      val result =
        amendmentAuthAction.invokeBlock(authRequest(Headers(), agent), okResponseGenerator)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/ppt-accounts-url")
    }

  }
}
