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

package controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import views.html.UpdateCompaniesHouseView
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class UpdateCompaniesHouseControllerSpec extends ControllerSpec {

  val mockView: UpdateCompaniesHouseView = mock[UpdateCompaniesHouseView]
  when(mockView.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Test View"))

  val sut = new UpdateCompaniesHouseController(
    journeyAction = spyJourneyAction,
    mockRegistrationConnector,
    mockView,
    stubMessagesControllerComponents().messagesApi
  )

  "onPageLoad" should {
    "return the view" in {

      spyJourneyAction.setReg(aRegistration(withIncorpDetails(incorporationDetails)))
      val resultF = sut.onPageLoad()(FakeRequest())

      status(resultF) mustBe OK
      contentAsString(resultF) mustBe "Test View"
      verify(mockView).apply(refEq(testCompanyNumber), refEq(testCompanyName))(any(), any())
    }
  }

  "reset" should {
    "clear the cache and redirect to task list" in {

      spyJourneyAction.setReg(aRegistration(withIncorpDetails(incorporationDetails)))
      val updated = aRegistration(withIncorpDetails(incorporationDetails)).clearAddressFromGrs

      when(mockRegistrationConnector.update(any())(any()))
        .thenReturn(Future.successful(Right(updated)))
      val resultF = sut.reset()(FakeRequest())

      status(resultF) mustBe SEE_OTHER
      redirectLocation(resultF) mustBe Some(routes.TaskListController.displayPage().url)
      verify(mockRegistrationConnector).update(refEq(updated))(any())
    }
  }

}
