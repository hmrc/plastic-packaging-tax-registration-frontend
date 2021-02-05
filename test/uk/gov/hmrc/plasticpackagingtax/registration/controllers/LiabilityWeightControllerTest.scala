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
import controllers.Assets.{BAD_REQUEST, SEE_OTHER}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_weight_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class LiabilityWeightControllerTest extends ControllerSpec {

  private val page = mock[liability_weight_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new LiabilityWeightController(authenticate = mockAuthAction,
                                  mockJourneyAction,
                                  mcc = mcc,
                                  liability_weight_page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[LiabilityWeight]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Liability Weight Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(FakeRequest("GET", "/"))

        status(result) mustBe OK.intValue
      }
    }

    "return 200 (OK)" when {
      "user submits the liability total weight" in {
        authorizedUser()
        val result = controller.submit()(
          FakeRequest("POST", "")
            .withJsonBody(Json.toJson(LiabilityWeight(Some(1000))))
            .withCSRFToken
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user submits invalid liability weight" in {
        authorizedUser()
        val result = controller.submit()(
          FakeRequest("POST", "")
            .withJsonBody(Json.toJson(LiabilityWeight(Some(999))))
            .withCSRFToken
        )

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error when user not authorised" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(FakeRequest("GET", "/"))

        intercept[RuntimeException](status(result))
      }
    }
  }
}
