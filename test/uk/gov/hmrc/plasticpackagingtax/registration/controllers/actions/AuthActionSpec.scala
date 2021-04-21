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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

import base.{MetricsMocks, PptTestData}
import base.unit.ControllerSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{Headers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import scala.concurrent.Future

class AuthActionSpec extends ControllerSpec with MetricsMocks {

  private def createAuthAction(
    utrAllowedList: UtrAllowedList = new UtrAllowedList(Seq.empty)
  ): AuthAction =
    new AuthActionImpl(mockAuthConnector,
                       utrAllowedList,
                       metricsMock,
                       stubMessagesControllerComponents()
    )

  private val okResponseGenerator = (_: AuthenticatedRequest[_]) => Future(Results.Ok)

  "Auth Action" should {

    "return InsufficientEnrolments when UTR number is missing" in {
      val user = PptTestData.newUser("123", Some(""))
      authorizedUser(user)

      intercept[InsufficientEnrolments] {
        await(createAuthAction().invokeBlock(authRequest(Headers(), user), okResponseGenerator))
      }
    }

    "process request when UTR number is present" in {
      val user = PptTestData.newUser("123", Some("555"))
      authorizedUser(user)

      await(
        createAuthAction().invokeBlock(authRequest(Headers(), user), okResponseGenerator)
      ) mustBe Results.Ok
    }

    "time calls to authorisation" in {
      val user = PptTestData.newUser("123", Some("555"))
      authorizedUser(user)

      await(createAuthAction().invokeBlock(authRequest(Headers(), user), okResponseGenerator))
      metricsMock.defaultRegistry.timer(
        "ppt.registration.upstream.auth.timer"
      ).getCount should be > 1L
    }

    "process request when UTR number is present and allowed" in {
      val utr  = "555"
      val user = PptTestData.newUser("123", Some(utr))
      authorizedUser(user)

      await(
        createAuthAction(new UtrAllowedList(Seq(utr))).invokeBlock(authRequest(Headers(), user),
                                                                   okResponseGenerator
        )
      ) mustBe Results.Ok
    }

    "redirect to home when UTR number is present but not allowed" in {
      val user = PptTestData.newUser("123", Some("555"))
      authorizedUser(user)

      val result =
        createAuthAction(new UtrAllowedList(Seq("someOtherUtr"))).invokeBlock(
          authRequest(Headers(), user),
          okResponseGenerator
        )

      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
    }
  }
}
