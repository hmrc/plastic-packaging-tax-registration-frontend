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

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user submits the liability total weight" when {

          "and weight is greater than minimum weight and feature flag 'liabilityPreLaunch' is disabled" in {
            authorizedUser()
            mockRegistrationFind(aRegistration())
            mockRegistrationUpdate()

            val result =
              controller.submit()(postRequestEncoded(LiabilityWeight(Some(20000)), formAction))

            status(result) mustBe SEE_OTHER

            modifiedRegistration.liabilityDetails.weight mustBe Some(LiabilityWeight(Some(20000)))
            modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe None

            formAction._1 match {
              case "SaveAndContinue" =>
                redirectLocation(result) mustBe Some(
                  routes.LiabilityStartDateController.displayPage().url
                )
              case _ =>
                redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
            }
          }
          "and weight is less than minimum weight" in {
            authorizedUser()
            mockRegistrationFind(aRegistration())
            mockRegistrationUpdate()
            val result =
              controller.submit()(postRequestEncoded(LiabilityWeight(Some(2000)), formAction))

            status(result) mustBe SEE_OTHER

            modifiedRegistration.liabilityDetails.weight mustBe Some(LiabilityWeight(Some(2000)))
            modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(true)

            formAction._1 match {
              case "SaveAndContinue" =>
                redirectLocation(result) mustBe Some(
                  routes.ExpectToExceedThresholdWeightController.displayPage().url
                )
              case _ =>
                redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
            }
          }

          "and weight is not set" in {
            authorizedUser()
            mockRegistrationFind(aRegistration())
            mockRegistrationUpdate()
            when(config.isDefaultFeatureFlagEnabled(refEq(Features.isPreLaunch))).thenReturn(true)

            val result =
              controller.submit()(postRequestEncoded(LiabilityWeight(None), formAction))

            status(result) mustBe BAD_REQUEST

            formAction._1 match {
              case "SaveAndContinue" =>
                redirectLocation(result) mustBe None
              case _ =>
                redirectLocation(result) mustBe None
            }
          }
        }
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user submits invalid liability weight" in {
        authorizedUser()
        val result =
          controller.submit()(postRequest(Json.toJson(LiabilityWeight(Some(100000000000L)))))

        status(result) mustBe BAD_REQUEST
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
}
