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

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.helpers.LiabilityLinkHelper
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_start_date_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class LiabilityStartDateControllerTest extends ControllerSpec {

  private val page                    = mock[liability_start_date_page]
  private val mcc                     = stubMessagesControllerComponents()
  private val liabilityBacklinkHelper = mock[LiabilityLinkHelper]

  private val controller =
    new LiabilityStartDateController(authenticate = mockAuthAction,
                                     mockJourneyAction,
                                     mockRegistrationConnector,
                                     mcc = mcc,
                                     page = page,
                                     liabilityBacklinkHelper = liabilityBacklinkHelper
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[Date]], any[Call])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(page)
  }

  "Liability Start Date Controller" should {

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

      "display page method is invoked and registration model contains data" in {
        authorizedUser()
        mockRegistrationUpdate()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user submits the liability start date" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()
          val result =
            controller.submit()(postRequestEncoded(Date(Some(1), Some(6), Some(2022)), formAction))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.liabilityDetails.startDate mustBe Some(
            Date(Some(1), Some(6), Some(2022))
          )
          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.CheckLiabilityDetailsAnswersController.displayPage().url
              )
            case _ =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }
        }
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user submits invalid liability start date" in {
        authorizedUser()
        val result =
          controller.submit()(postRequest(Json.toJson(Date(Some(1), Some(4), Some(1900)))))

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
        val result =
          controller.submit()(postRequest(Json.toJson(Date(Some(1), Some(4), Some(2022)))))

        intercept[DownstreamServiceError](status(result))
      }

      "user submits form and a registration update runtime exception occurs" in {
        authorizedUser()
        mockRegistrationException()
        val result =
          controller.submit()(postRequest(Json.toJson(Date(Some(1), Some(4), Some(2022)))))

        intercept[RuntimeException](status(result))
      }
    }
  }
}
