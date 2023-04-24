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
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import connectors.DownstreamServiceError
import forms.liability.LiabilityWeight
import views.html.liability.liability_weight_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class LiabilityWeightControllerTest extends ControllerSpec {

  private val page = mock[liability_weight_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new LiabilityWeightController(journeyAction = spyJourneyAction,
                                  mockRegistrationConnector,
                                  mcc = mcc,
                                  page = page
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
        spyJourneyAction.setReg(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {

        spyJourneyAction.setReg(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "update registration and redirect as expected" when {
      "user enters projected 12m weight" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()

        val result = controller.submit()(postRequestEncoded(LiabilityWeight(Some(20000))))

        status(result) mustBe SEE_OTHER

        modifiedRegistration.liabilityDetails.expectedWeightNext12m mustBe Some(
          LiabilityWeight(Some(20000))
        )

        redirectLocation(result) mustBe Some(routes.RegistrationTypeController.displayPage().url)
      }
    }

    "redisplay page" when {
      "does not provide projected 12m weight" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()

        val result = controller.submit()(postRequestEncoded(LiabilityWeight(None)))

        status(result) mustBe BAD_REQUEST
      }
      "user provides an out of range projected 12m weight" in {

        val result =
          controller.submit()(postRequest(Json.toJson(LiabilityWeight(Some(100000000000L)))))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "return an error" when {

    "user submits form and the registration update fails" in {

      mockRegistrationUpdateFailure()

      intercept[DownstreamServiceError](status(
        controller.submit()(postRequest(Json.toJson(LiabilityWeight(Some(1000)))))
      ))
    }

    "user submits form and a registration update runtime exception occurs" in {

      mockRegistrationException()

      intercept[RuntimeException](status(
        controller.submit()(postRequest(Json.toJson(LiabilityWeight(Some(1000)))))
      ))
    }
  }
}
