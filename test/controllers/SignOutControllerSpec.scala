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

package controllers

import akka.http.scaladsl.model.StatusCodes.OK
import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, session, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.session_timed_out
import views.viewmodels.SignOutReason

class SignOutControllerSpec extends ControllerSpec {

  private val mcc                = stubMessagesControllerComponents()
  private val sessionTimeoutPage = mock[session_timed_out]

  private val controller =
    new SignOutController(config, sessionTimeoutPage, mcc)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(sessionTimeoutPage.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(sessionTimeoutPage)
    super.afterEach()
  }

  "SignOutController signOut function" should {

    "redirect to exit survey" when {

      "auth user signs out" in {

        val exitSurveyUrl: String = givenExistSurveyUrl

        val result = controller.signOut(SignOutReason.UserAction)(FakeRequest())

        redirectLocation(result) mustBe Some(exitSurveyUrl)

      }

      "unauth user signs out" in {

        val exitSurveyUrl: String = givenExistSurveyUrl

        val result = controller.signOut(SignOutReason.UserAction)(FakeRequest())

        redirectLocation(result) mustBe Some(exitSurveyUrl)

      }
    }

    "return 303 (SEE_OTHER) status" when {

      "auth user signs out" in {

        val result = controller.signOut(SignOutReason.UserAction)(FakeRequest())

        status(result) mustBe SEE_OTHER

      }

      "unauth user signs out" in {

        val result = controller.signOut(SignOutReason.UserAction)(FakeRequest())

        status(result) mustBe SEE_OTHER

      }
    }

    "creates new blank session" when {

      "auth user signs out" in {

        val result = controller.signOut(SignOutReason.UserAction)(FakeRequest().withSession("keyA" -> "valueA"))

        session(result).get("keyA") shouldBe None

      }

      "unauth user signs out" in {

        val result = controller.signOut(SignOutReason.UserAction)(FakeRequest().withSession("keyA" -> "valueA"))

        session(result).get("keyA") shouldBe None

      }
    }

  }

  "SignOutController sessionTimeout function" should {

    "return 303 (SEE_OTHER) status" when {

      "auth user session times out" in {

        val result = controller.signOut(SignOutReason.SessionTimeout)(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SignOutController.sessionTimeoutSignedOut().url)

      }

      "unauth user session times out" in {

        val result = controller.signOut(SignOutReason.SessionTimeout)(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SignOutController.sessionTimeoutSignedOut().url)

      }
    }

    "creates new blank session" when {

      "session times out" in {

        val result =
          controller.signOut(SignOutReason.SessionTimeout)(FakeRequest().withSession("keyA" -> "valueA"))

        session(result).get("keyA") shouldBe None
      }
    }

    "return 200 (OK) status" when {

      "unauthorised user hits /we-sign-you-out page" in {

        val result = controller.sessionTimeoutSignedOut()(FakeRequest())

        status(result) mustBe OK.intValue
      }
    }

  }

  private def givenExistSurveyUrl = {
    val exitSurveyUrl = "http://localhost:9514/feedback/plastic-packaging-tax-registration"
    when(config.exitSurveyUrl).thenReturn(exitSurveyUrl)
    exitSurveyUrl
  }

}
