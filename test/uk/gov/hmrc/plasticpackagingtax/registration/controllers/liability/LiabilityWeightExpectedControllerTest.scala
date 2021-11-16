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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityExpectedWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_weight_expected_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class LiabilityWeightExpectedControllerTest extends ControllerSpec {

  private val page = mock[liability_weight_expected_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new LiabilityWeightExpectedController(authenticate = mockAuthAction,
                                          mockJourneyAction,
                                          mockRegistrationConnector,
                                          mcc = mcc,
                                          page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[LiabilityExpectedWeight]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Liability Weight Expected Controller" should {

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
      "redirect and update registration for " + formAction._1 when {
        "user selects 'no'" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> "no", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.liabilityDetails.expectedWeight mustBe Some(
            LiabilityExpectedWeight(Some(false), None)
          )

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(routes.NotLiableController.displayPage().url)
            case _ =>
              redirectLocation(result) mustBe Some(routes.NotLiableController.displayPage().url)
          }
        }

        "group registration enabled and weight is greater than de minimis" in {
          authorizedUser(features = Map(Features.isGroupRegistrationEnabled -> true))
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> "yes", "totalKg" -> "10000", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.liabilityDetails.expectedWeight mustBe Some(
            LiabilityExpectedWeight(Some(true), Some(10000))
          )

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.RegistrationTypeController.displayPage().url
              )
            case _ =>
              redirectLocation(result) mustBe Some(
                routes.RegistrationTypeController.displayPage().url
              )
          }
        }

        "group registration not enabled and weight is greater than de minimis" in {
          authorizedUser(features = Map(Features.isGroupRegistrationEnabled -> false))
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> "yes", "totalKg" -> "10000", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.liabilityDetails.expectedWeight mustBe Some(
            LiabilityExpectedWeight(Some(true), Some(10000))
          )

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                pptRoutes.RegistrationController.displayPage().url
              )
            case _ =>
              redirectLocation(result) mustBe Some(
                pptRoutes.RegistrationController.displayPage().url
              )
          }
        }

      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user submits invalid liability weight" in {
        authorizedUser()
        val correctForm =
          Seq("answer" -> "yes", "totalKg" -> "10000000000", saveAndContinueFormAction)
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe BAD_REQUEST
      }

      "user submits liability weight below threshold" in {
        authorizedUser()
        val correctForm = Seq("answer" -> "yes", "totalKg" -> "10", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

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
        val correctForm = Seq("answer" -> "yes", "totalKg" -> "20000", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[DownstreamServiceError](status(result))
      }

      "user submits form and a registration update runtime exception occurs" in {
        authorizedUser()
        mockRegistrationException()
        val correctForm = Seq("answer" -> "yes", "totalKg" -> "20000", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[RuntimeException](status(result))
      }
    }
  }
}
