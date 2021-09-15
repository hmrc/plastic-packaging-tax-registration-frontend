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

package uk.gov.hmrc.plasticpackagingtax.registration.config

import base.unit.UnitViewSpec
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.SEE_OTHER
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{redirectLocation, status, stubMessagesApi}
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, NoActiveSession}
import uk.gov.hmrc.hmrcfrontend.views.Utils.urlEncode
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.error_template

import scala.concurrent.Future

class ErrorHandlerTest
    extends UnitViewSpec with Matchers with DefaultAwaitTimeout with OptionValues {

  private val errorPage    = instanceOf[error_template]
  private val errorHandler = new ErrorHandler(errorPage, stubMessagesApi())(appConfig)

  "ErrorHandlerSpec" should {

    "standardErrorTemplate" in {

      val result =
        errorHandler.standardErrorTemplate("title", "heading", "message")(journeyRequest).body

      result must include("title")
      result must include("heading")
      result must include("message")
    }

    "handle no active session authorisation exception" in {

      when(appConfig.loginUrl).thenReturn("http://localhost:9949/auth-login-stub/gg-sign-in")
      when(appConfig.loginContinueUrl).thenReturn(
        "http://localhost:8503/plastic-packaging-tax/registration"
      )

      val error            = new NoActiveSession("A user is not logged in") {}
      val result           = Future.successful(errorHandler.resolveError(journeyRequest, error))
      val encodedTargetUrl = urlEncode("http://localhost:8503/plastic-packaging-tax/registration")
      val expectedLocation =
        s"http://localhost:9949/auth-login-stub/gg-sign-in?continue=$encodedTargetUrl"

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(expectedLocation)
    }
    "handle insufficient enrolments authorisation exception" in {

      val error  = InsufficientEnrolments("HMRC-PPT-ORG")
      val result = Future.successful(errorHandler.resolveError(journeyRequest, error))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must endWith("/unauthorised")
    }
  }
}
