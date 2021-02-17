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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import akka.http.scaladsl.model.StatusCodes.OK
import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{redirectLocation, session, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.session_timed_out
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.SignOutReason
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class SignOutControllerSpec extends ControllerSpec {

  private val mcc                = stubMessagesControllerComponents()
  private val sessionTimeoutPage = mock[session_timed_out]
  private val controller         = new SignOutController(mockAuthAction, config, sessionTimeoutPage, mcc)

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

        authorizedUser()
        val exitSurveyUrl: String = givenExistSurveyUrl

        val result = controller.signOut(SignOutReason.UserAction)(getRequest())

        redirectLocation(result) mustBe Some(exitSurveyUrl)

      }
    }

    "return 303 (SEE_OTHER) status" when {

      "auth user signs out" in {

        authorizedUser()

        val result = controller.signOut(SignOutReason.UserAction)(getRequest())

        status(result) mustBe SEE_OTHER

      }
    }

    "creates new blank session" when {

      "auth user signs out" in {

        authorizedUser()

        val result = controller.signOut(SignOutReason.UserAction)(getRequest("keyA" -> "valueA"))

        session(result).get("keyA") shouldBe None

      }
    }

    "throw exception" when {

      "unauthorised user hits sign out url" in {

        unAuthorizedUser()

        val result = controller.signOut(SignOutReason.UserAction)(getRequest())

        intercept[RuntimeException](status(result))

      }
    }
  }

  "SignOutController sessionTimeout function" should {

    "return 303 (SEE_OTHER) status" when {

      "session times out" in {

        authorizedUser()

        val result = controller.signOut(SignOutReason.SessionTimeout)(getRequest())

        status(result) mustBe SEE_OTHER

      }
    }

    "redirect to session timed out page" when {

      "session times out" in {

        authorizedUser()

        val result = controller.signOut(SignOutReason.SessionTimeout)(getRequest())

        redirectLocation(result) mustBe Some(routes.SignOutController.sessionTimeoutSignedOut().url)
      }
    }

    "creates new blank session" when {

      "session times out" in {

        authorizedUser()

        val result =
          controller.signOut(SignOutReason.SessionTimeout)(getRequest("keyA" -> "valueA"))

        session(result).get("keyA") shouldBe None
      }
    }

    "return 200 (OK) status" when {

      "unauthorised user hits /we-sign-you-out page" in {

        unAuthorizedUser()

        val result = controller.sessionTimeoutSignedOut()(getRequest())

        status(result) mustBe OK.intValue
      }
    }

    "throws exception" when {

      "unauthorised user hits session timeout url" in {

        unAuthorizedUser()

        val result = controller.signOut(SignOutReason.SessionTimeout)(getRequest())

        intercept[RuntimeException](status(result))

      }
    }
  }

  private def givenExistSurveyUrl = {
    val exitSurveyUrl = "http://localhost:9514/feedback/plastic-packaging-tax-registration"
    when(config.exitSurveyUrl).thenReturn(exitSurveyUrl)
    exitSurveyUrl
  }

}
