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
import controllers.Assets.SEE_OTHER
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.ControllerSpec
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.honesty_declaration
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class HonestyDeclarationControllerSpec extends ControllerSpec {
  private val page        = mock[honesty_declaration]
  private val fakeRequest = FakeRequest("GET", "/")
  private val mcc         = stubMessagesControllerComponents()

  private val controller =
    new HonestyDeclarationController(authenticate = mockAuthAction,
                                     mcc = mcc,
                                     honesty_declaration = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Honesty Declaration Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(fakeRequest)

        status(result) mustBe OK.intValue
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(fakeRequest)

        intercept[RuntimeException](status(result))
      }
    }

    "return 303 (SEE_OTHER) and redirect to honesty declaration" when {
      "user submits the honesty declaration form" in {
        authorizedUser()
        val result = controller.submit()(FakeRequest("POST", ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HonestyDeclarationController.displayPage().url)
      }
    }
  }
}
