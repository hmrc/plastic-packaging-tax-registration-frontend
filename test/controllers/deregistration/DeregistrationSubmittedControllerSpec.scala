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

package controllers.deregistration

import base.unit.{ControllerSpec, MockDeregistrationDetailRepository}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.deregistration.deregistration_submitted_page

class DeregistrationSubmittedControllerSpec
    extends ControllerSpec with MockDeregistrationDetailRepository with PptTestData {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[deregistration_submitted_page]
  when(mockPage.apply()(any(), any())).thenReturn(HtmlFormat.raw("Deregistration Submitted"))

  private val deregistrationSubmittedController =
    new DeregistrationSubmittedController(FakeAmendAuthAction, mcc, mockPage)

  "Deregistration Submitted Controller" should {
    "display page" when {
      "user authenticated" in {


        val resp = deregistrationSubmittedController.displayPage()(FakeRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "Deregistration Submitted"
      }
    }
  }
}
