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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExpectToExceedThresholdWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_expect_to_exceed_threshold_weight_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class LiabilityExpectToExceedThresholdWeightControllerSpec extends ControllerSpec {
  private val page = mock[liability_expect_to_exceed_threshold_weight_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new LiabilityExpectToExceedThresholdWeightController(authenticate = mockAuthAction,
                                                         mockJourneyAction,
                                                         mockRegistrationConnector,
                                                         mcc = mcc,
                                                         page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[ExpectToExceedThresholdWeight]])(any(), any())).thenReturn(
      HtmlFormat.empty
    )
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Liability Expect To Exceed Threshold Weight Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        val registration =
          aRegistration(
            withLiabilityDetails(LiabilityDetails(expectToExceedThresholdWeight = Some(true)))
          )
        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method without the expectToExceedThresholdWeight not set" in {
        val registration =
          aRegistration(
            withLiabilityDetails(LiabilityDetails(expectToExceedThresholdWeight = None))
          )
        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user submits 'Yes' answer with isPreLaunch enabled" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> "yes", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(true)

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.LiabilityStartDateController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }

        "user submits 'Yes' answer with isPreLaunch disabled" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> "yes", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(true)

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.LiabilityStartDateController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }

        "user submits 'No' answer" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> "no", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(false)

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(routes.NotLiableController.displayPage().url)
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }
      }

      "return 400 (BAD_REQUEST) for " + formAction._1 when {
        "user does not pick an answer" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          val result =
            controller.submit()(postRequestEncoded(JsObject.empty, formAction))

          status(result) mustBe BAD_REQUEST
        }

        "user enters invalid data" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          val incorrectForm = Seq("answer" -> "maybe", formAction)
          val result        = controller.submit()(postJsonRequestEncoded(incorrectForm: _*))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return an error for " + formAction._1 when {

        "user is not authorised" in {
          unAuthorizedUser()
          val result = controller.displayPage()(getRequest())

          intercept[RuntimeException](status(result))
        }

        "user submits form and the registration update fails" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdateFailure()

          val correctForm = Seq("answer" -> "yes", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationException()

          val correctForm = Seq("answer" -> "yes", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[RuntimeException](status(result))
        }
      }
    }
  }
}
