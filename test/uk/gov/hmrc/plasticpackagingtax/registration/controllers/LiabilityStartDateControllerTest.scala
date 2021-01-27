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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import controllers.Assets.{BAD_REQUEST, SEE_OTHER}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import base.unit.ControllerSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_start_date_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class LiabilityStartDateControllerTest extends ControllerSpec {

  private val page        = mock[liability_start_date_page]
  private val fakeRequest = FakeRequest("GET", "/")
  private val mcc         = stubMessagesControllerComponents()

  private val controller =
    new LiabilityStartDateController(authenticate = mockAuthAction,
                                     mcc = mcc,
                                     liability_start_date_page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[Date]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Liability Start Date Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(fakeRequest)

        status(result) mustBe OK.intValue
      }
    }

    "return 200 (OK)" when {
      "user submits the liability start date" in {
        authorizedUser()
        val result = controller.submit()(
          FakeRequest("POST", "")
            .withJsonBody(Json.toJson(Date(Some(1), Some(4), Some(2022))))
            .withCSRFToken
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user submits invalid liability start date" in {
        authorizedUser()
        val result = controller.submit()(
          FakeRequest("POST", "")
            .withJsonBody(Json.toJson(Date(Some(1), Some(4), Some(1900))))
            .withCSRFToken
        )

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error when user not authorised" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(fakeRequest)

        intercept[RuntimeException](status(result))
      }
    }
  }
}
