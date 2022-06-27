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
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExpectToExceedThresholdWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.expect_to_exceed_threshold_weight_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ExpectToExceedThresholdWeightControllerSpec extends ControllerSpec {

  val mockPage: expect_to_exceed_threshold_weight_page = {
    mock[expect_to_exceed_threshold_weight_page]
  }
  val mockFormProvider = mock[ExpectToExceedThresholdWeight]
  val mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  val controller: ExpectToExceedThresholdWeightController =
    new ExpectToExceedThresholdWeightController(authenticate = mockAuthAction,
      mockJourneyAction,
      mockRegistrationConnector,
      mcc = mcc,
      page = mockPage,
      form = mockFormProvider
    )

  when(mockPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)

  "displayPage" should {
    "return 200 (OK)" when {

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
  }

  "submit" should {
    "return 303 (REDIRECT)" when {
      "user submits 'Yes' answer" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("answer" -> "yes")
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(true)

        redirectLocation(result) mustBe Some(
          routes.ExpectToExceedThresholdWeightDateController.displayPage().url
        )
      }

      "user submits 'No' answer" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("answer" -> "no")
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER

        modifiedRegistration.liabilityDetails.expectToExceedThresholdWeight mustBe Some(false)

        redirectLocation(result) mustBe Some(routes.NotLiableController.displayPage().url)
      }

      "return 400 (BAD_REQUEST)" when {
        "the form fails to bind" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          val result =
            controller.submit()(postRequestEncoded(JsObject.empty))

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
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdateFailure()

          val correctForm = Seq("answer" -> "yes")
          val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationException()

          val correctForm = Seq("answer" -> "yes")
          val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[RuntimeException](status(result))
        }
      }
    }
  }
}
