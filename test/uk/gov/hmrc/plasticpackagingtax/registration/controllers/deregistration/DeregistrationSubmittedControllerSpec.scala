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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.deregistration

import base.unit.{ControllerSpec, MockDeregistrationDetailRepository}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregistration_submitted_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class DeregistrationSubmittedControllerSpec
    extends ControllerSpec with MockDeregistrationDetailRepository with PptTestData {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[deregistration_submitted_page]
  when(mockPage.apply()(any(), any())).thenReturn(HtmlFormat.raw("Deregistration Submitted"))

  private val deregistrationSubmittedController =
    new DeregistrationSubmittedController(mockEnrolledAuthAction, mcc, mockPage)

  "Deregistration Submitted Controller" should {
    "display page" when {
      "user authenticated" in {
        authorizedUser()

        val resp = deregistrationSubmittedController.displayPage()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "Deregistration Submitted"
      }
    }

    "throw exception" when {
      "user not authenticated" in {
        unAuthorizedUser()

        intercept[RuntimeException] {
          await(deregistrationSubmittedController.displayPage()(getRequest()))
        }
      }
    }
  }
}
