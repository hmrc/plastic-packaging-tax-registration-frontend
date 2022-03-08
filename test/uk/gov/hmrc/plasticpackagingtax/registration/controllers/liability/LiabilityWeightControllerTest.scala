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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_weight_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class LiabilityWeightControllerTest extends ControllerSpec {

  private val page = mock[liability_weight_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new LiabilityWeightController(authenticate = mockAuthAction,
                                  mockJourneyAction,
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
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "update registration and redirect as expected" when {
      "user enters projected 12m weight" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val result = controller.submit()(postRequestEncoded(LiabilityWeight(Some(20000))))

        status(result) mustBe SEE_OTHER

        modifiedRegistration.liabilityDetails.weight mustBe Some(LiabilityWeight(Some(20000)))
        modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe None

        redirectLocation(result) mustBe Some(routes.RegistrationTypeController.displayPage().url)
      }
    }

    "redisplay page" when {
      "does not provide projected 12m weight" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()
        when(config.isDefaultFeatureFlagEnabled(refEq(Features.isPreLaunch))).thenReturn(true)

        val result = controller.submit()(postRequestEncoded(LiabilityWeight(None)))

        status(result) mustBe BAD_REQUEST
      }
      "user provides an out of range projected 12m weight" in {
        authorizedUser()
        val result =
          controller.submit()(postRequest(Json.toJson(LiabilityWeight(Some(100000000000L)))))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "return an error" when {

    "user is not authorised" in {
      unAuthorizedUser()
      val result = controller.displayPage()(getRequest())

      intercept[RuntimeException](status(result))
    }

    "user submits form and the registration update fails" in {
      authorizedUser()
      mockRegistrationUpdateFailure()
      val result = controller.submit()(postRequest(Json.toJson(LiabilityWeight(Some(1000)))))

      intercept[DownstreamServiceError](status(result))
    }

    "user submits form and a registration update runtime exception occurs" in {
      authorizedUser()
      mockRegistrationException()
      val result = controller.submit()(postRequest(Json.toJson(LiabilityWeight(Some(1000)))))

      intercept[RuntimeException](status(result))
    }
  }
}
