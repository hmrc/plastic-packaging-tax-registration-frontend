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
import connectors.DownstreamServiceError
import forms.Date
import forms.liability.{ExceededThresholdWeight, ExceededThresholdWeightAnswer}
import models.registration.Registration
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{RETURNS_DEEP_STUBS, reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.libs.json.JsObject
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.liability.exceeded_threshold_weight_page

import java.time.LocalDate
import scala.concurrent.Future

class ExceededThresholdWeightControllerSpec extends ControllerSpec with BeforeAndAfterEach{

  val mockPage: exceeded_threshold_weight_page = mock[exceeded_threshold_weight_page]


  val mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  val form = inject[ExceededThresholdWeight]

  val controller = new ExceededThresholdWeightController(
    authenticate = mockAuthAction,
    mockJourneyAction,
    appConfig,
    mockRegistrationConnector,
    mcc = mcc,
    form,
    page = mockPage
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockPage, appConfig)

    when(mockPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  "displayPage" when {

    "user is authorised" should {
      "return 200" in {
        authorizedUser()
        val result: Future[Result] = controller.displayPage()(getRequest())
        status(result) shouldEqual Status.OK
      }
    }

    "continuing an existing registration" should {

      "populate the form with the previous answer" in {
        authorizedUser()
        val existingRegistration = mock[Registration](RETURNS_DEEP_STUBS)
        when(existingRegistration.liabilityDetails.exceededThresholdWeight).thenReturn(Some(false))
        when(existingRegistration.liabilityDetails.dateExceededThresholdWeight).thenReturn(None)
        when(mockRegistrationConnector.find(any[String])(any())).thenReturn(
          Future.successful(Right(Some(existingRegistration)))
        )

        val result = controller.displayPage()(getRequest())
        status(result) mustBe Status.OK

        verify(existingRegistration.liabilityDetails).exceededThresholdWeight

        val captor = ArgumentCaptor.forClass(classOf[Form[Boolean]])
        verify(mockPage).apply(captor.capture(), any())(any(), any())
        val form: Form[Boolean] = captor.getValue
        form.value shouldBe Some(ExceededThresholdWeightAnswer(false, None))
      }
    }

    "should pass feature flag to view" in {
      authorizedUser()

      val existingRegistration = mock[Registration](RETURNS_DEEP_STUBS)
      when(existingRegistration.liabilityDetails.exceededThresholdWeight).thenReturn(Some(false))
      when(existingRegistration.liabilityDetails.dateExceededThresholdWeight).thenReturn(None)
      when(appConfig.isBackwardLookChangeEnabled).thenReturn(true)

      await(controller.displayPage()(getRequest()))

      verify(appConfig).isBackwardLookChangeEnabled
      verify(mockPage).apply(any(), ArgumentMatchers.eq(true))(any(),any())
    }
  }

  "submit" should {
    authorizedUser()

    "update registration and redirect" when {

      "user answers no" in {
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()
        val correctForm = Seq("answer" -> "no")
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))
        status(result) mustBe SEE_OTHER
        modifiedRegistration.liabilityDetails.exceededThresholdWeight mustBe Some(false)
        redirectLocation(result) mustBe Some(
          routes.TaxStartDateController.displayPage().url
        )
      }

      "user answers yes" in {
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()
        val correctForm = Seq("answer" -> "yes",
          "exceeded-threshold-weight-date.day" -> "5",
          "exceeded-threshold-weight-date.month" -> "5",
          "exceeded-threshold-weight-date.year" -> "2022")

        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))
        status(result) mustBe SEE_OTHER
        modifiedRegistration.liabilityDetails.exceededThresholdWeight mustBe Some(true)
        modifiedRegistration.liabilityDetails.dateExceededThresholdWeight mustBe Some(Date(LocalDate.of(2022,5,5)))
        redirectLocation(result) mustBe Some(
          routes.TaxStartDateController.displayPage().url
        )
      }
    }

    "should pass feature flag to view on error" in {
      when(appConfig.isBackwardLookChangeEnabled).thenReturn(true)
      await(controller.submit()(postRequestEncoded(JsObject.empty)))

      verify(appConfig).isBackwardLookChangeEnabled
      verify(mockPage).apply(any(), ArgumentMatchers.eq(true))(any(),any())
    }
    "return an error" when {
      "the form fails to bind" in {
        val result = controller.submit()(postRequestEncoded(JsObject.empty))
        status(result) mustBe Status.BAD_REQUEST
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdateFailure()

        val correctForm = Seq("answer" -> "yes",
          "exceeded-threshold-weight-date.day" -> "5",
          "exceeded-threshold-weight-date.month" -> "5",
          "exceeded-threshold-weight-date.year" -> "2022")
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[DownstreamServiceError](await(result))
      }

      "user submits form and a registration update runtime exception occurs" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationException()

        val correctForm = Seq("answer" -> "yes",
          "exceeded-threshold-weight-date.day" -> "5",
          "exceeded-threshold-weight-date.month" -> "5",
          "exceeded-threshold-weight-date.year" -> "2022")
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[RuntimeException](await(result))
      }

    }

  }

  "In general" when {
    "the user is unauthorised" should {
      object TestingException extends Exception

      "displayPage will result in an exception" in {
        when(mockAuthConnector.authorise(any(), any())(any(), any())).thenReturn(
          Future.failed(TestingException)
        )
        val result = controller.displayPage()(getRequest())
        intercept[TestingException.type](await(result))
      }

      "submit will result in an exception" in {
        when(mockAuthConnector.authorise(any(), any())(any(), any())).thenReturn(
          Future.failed(TestingException)
        )
        val result: Future[Result] = controller.submit()(getRequest())
        intercept[TestingException.type](await(result))
      }

    }
  }
}
