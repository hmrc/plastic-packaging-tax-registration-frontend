/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.enrolment

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.MockitoSugar.reset
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.enrolment.confirmation_page

class ConfirmationControllerSpec extends ControllerSpec {
  private val page = mock[confirmation_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ConfirmationController(authenticate = FakeAmendAuthAction, mcc = mcc, page = page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    given(page.apply()(any(), any())).willReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Confirmation controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "user is already enrolled and display page method is invoked" in {

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

    }

  }

}
