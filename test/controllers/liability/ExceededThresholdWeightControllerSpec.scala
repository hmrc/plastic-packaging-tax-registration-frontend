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
import forms.liability.ExceededThresholdWeight
import models.registration.Registration
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.libs.json.JsObject
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.liability.exceeded_threshold_weight_page

import scala.concurrent.Future

class ExceededThresholdWeightControllerSpec extends ControllerSpec with BeforeAndAfterEach {

  val mockPage: exceeded_threshold_weight_page = mock[exceeded_threshold_weight_page]

  val mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  val form = inject[ExceededThresholdWeight]

  val controller = new ExceededThresholdWeightController(journeyAction = spyJourneyAction, config, mockRegistrationConnector, mcc = mcc, form, page = mockPage)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockPage, config)

    when(mockPage.apply(any())(any(), any())).thenReturn(HtmlFormat.raw("test view"))
  }

  "displayPage" when {

    "user is authorised" should {
      "return 200" in {
        spyJourneyAction.setReg(aRegistration())

        val result: Future[Result] = controller.displayPage()(FakeRequest())
        status(result) shouldEqual Status.OK
        contentAsString(result) mustBe "test view"
      }
    }

    "continuing an existing registration" should {

      "populate the form with the previous answer" in {
        val existingRegistration = mock[Registration](RETURNS_DEEP_STUBS)
        when(existingRegistration.liabilityDetails.exceededThresholdWeight).thenReturn(Some(false))
        spyJourneyAction.setReg(existingRegistration)

        val result = controller.displayPage()(FakeRequest())
        status(result) mustBe Status.OK

        verify(existingRegistration.liabilityDetails).exceededThresholdWeight

        val captor = ArgumentCaptor.forClass(classOf[Form[Boolean]])
        verify(mockPage).apply(captor.capture())(any(), any())
        val form: Form[Boolean] = captor.getValue
        form.value shouldBe Some(false)
      }
    }
  }

  "submit" should {

    "update registration and redirect" when {

      "user answers no" in {
        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()
        val correctForm = Seq("value" -> "no")
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.liabilityDetails.exceededThresholdWeight mustBe Some(false)
        redirectLocation(result) mustBe Some(routes.TaxStartDateController.displayPage().url)
      }

      "user answers yes" in {
        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()
        val correctForm = Seq("value" -> "yes")

        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))
        status(result) mustBe SEE_OTHER
        modifiedRegistration.liabilityDetails.exceededThresholdWeight mustBe Some(true)
        redirectLocation(result) mustBe Some(routes.ExceededThresholdWeightDateController.displayPage().url)
      }
    }

    "return an error" when {
      "the form fails to bind" in {
        val result = controller.submit()(postRequestEncoded(JsObject.empty))
        status(result) mustBe Status.BAD_REQUEST
      }

      "user submits form and the registration update fails" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdateFailure()

        val correctForm = Seq("value" -> "yes")

        intercept[DownstreamServiceError](await(controller.submit()(postJsonRequestEncoded(correctForm: _*))))
      }

      "user submits form and a registration update runtime exception occurs" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationException()

        val correctForm = Seq("value" -> "yes")

        intercept[RuntimeException](await(controller.submit()(postJsonRequestEncoded(correctForm: _*))))
      }

    }

  }

}
