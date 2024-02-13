/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.DownstreamServiceError
import forms.YesNoValues
import forms.liability.ExpectToExceedThresholdWeight
import models.registration.LiabilityDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.liability.expect_to_exceed_threshold_weight_page

class ExpectToExceedThresholdWeightControllerSpec extends ControllerSpec {

  val mockPage: expect_to_exceed_threshold_weight_page = mock[expect_to_exceed_threshold_weight_page]
  val mockFormProvider                                 = inject[ExpectToExceedThresholdWeight]
  val mcc: MessagesControllerComponents                = stubMessagesControllerComponents()

  val controller: ExpectToExceedThresholdWeightController =
    new ExpectToExceedThresholdWeightController(journeyAction = spyJourneyAction, mockRegistrationConnector, mcc = mcc, page = mockPage, form = mockFormProvider)

  when(mockPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)

  "displayPage" should {
    "return 200 (OK)" when {

      "user is authorised and display page method is invoked" in {

        spyJourneyAction.setReg(aRegistration())

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        val registration =
          aRegistration(withLiabilityDetails(LiabilityDetails(expectToExceedThresholdWeight = Some(true))))

        spyJourneyAction.setReg(registration)

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method without the expectToExceedThresholdWeight not set" in {
        val registration =
          aRegistration(withLiabilityDetails(LiabilityDetails(expectToExceedThresholdWeight = None)))

        spyJourneyAction.setReg(registration)

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }
    }
  }

  "submit" should {
    "return 303 (REDIRECT)" when {
      "user submits 'Yes' answer" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()

        val result = controller.submit()(postJsonRequestEncoded(createRequestBody: _*))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(true)
        redirectLocation(result).get mustBe controllers.liability.routes.ExpectToExceedThresholdWeightDateController.displayPage.url
      }

      "user submits 'No' answer" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("value" -> "no")
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(false)
        modifiedRegistration.liabilityDetails.dateRealisedExpectedToExceedThresholdWeight mustBe None
        redirectLocation(result).get mustBe controllers.liability.routes.ExceededThresholdWeightController.displayPage.url
      }

      "return 400 (BAD_REQUEST)" when {
        "the form fails to bind" in {

          spyJourneyAction.setReg(aRegistration())
          val result =
            controller.submit()(postRequestEncoded(JsObject.empty))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return an error" when {

        "user submits form and the registration update fails" in {

          spyJourneyAction.setReg(aRegistration())
          mockRegistrationUpdateFailure()

          intercept[DownstreamServiceError](status(controller.submit()(postJsonRequestEncoded(createRequestBody: _*))))
        }

        "user submits form and a registration update runtime exception occurs" in {

          spyJourneyAction.setReg(aRegistration())
          mockRegistrationException()

          intercept[RuntimeException](status(controller.submit()(postJsonRequestEncoded(createRequestBody: _*))))
        }
      }
    }
  }

  private def createRequestBody: Seq[(String, String)] =
    Seq("value" -> YesNoValues.YES)

}
