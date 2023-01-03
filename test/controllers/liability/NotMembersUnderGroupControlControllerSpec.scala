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

package controllers.liability

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.reset
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import views.html.liability.not_members_under_group_control_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class NotMembersUnderGroupControlControllerSpec extends ControllerSpec {
  private val page = mock[not_members_under_group_control_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new NotMembersUnderGroupControlController(authenticate = mockAuthAction,
                                              mcc = mcc,
                                              mockJourneyAction,
                                              page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val registration = aRegistration()
    mockRegistrationFind(registration)
    given(page.apply()(any(), any())).willReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Not members under group control " should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "return 303" when {

      "when form is submitted" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val result =
          controller.submit()(FakeRequest("POST", ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationTypeController.displayPage().url)
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }
  }

}
