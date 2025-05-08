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

package controllers.organisation

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import views.html.organisation.check_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class CheckAnswersControllerSpec extends ControllerSpec {

  private val page = mock[check_answers_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new CheckAnswersController(
      journeyAction = spyJourneyAction,
      registrationConnector = mockRegistrationConnector,
      mcc = mcc,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply()(any(), any())).thenReturn(HtmlFormat.raw("CYA Page"))
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Check Answers Controller" should {
    "display the check answers page" when {
      "user is authorised" in {
        spyJourneyAction.setReg(aRegistration())

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "CYA Page"
      }

    }
  }
}
