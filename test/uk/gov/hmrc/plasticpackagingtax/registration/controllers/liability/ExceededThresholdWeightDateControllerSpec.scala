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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExceededThresholdWeightDate
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.exceeded_threshold_weight_date_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

class ExceededThresholdWeightDateControllerSpec extends ControllerSpec {

  private val page = mock[exceeded_threshold_weight_date_page]
  private val mcc  = stubMessagesControllerComponents()

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val clock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private val exceedThresholdWeightDate =
    new ExceededThresholdWeightDate(mockAppConfig, clock)

  private val controller =
    new ExceededThresholdWeightDateController(authenticate = mockAuthAction,
                                              mockJourneyAction,
                                              mockRegistrationConnector,
                                              mcc = mcc,
                                              page = page,
                                              exceedThresholdWeightDate
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[Date]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(page)
  }

  "Exceeded Threshold Weight Date Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        val exceededDate = Date(LocalDate.of(2022, 4, 20))
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withLiabilityDetails(LiabilityDetails(dateExceededThresholdWeight = Some(exceededDate)))
          )
        )
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK

        val formCaptor: ArgumentCaptor[Form[Date]] = ArgumentCaptor.forClass(classOf[Form[Date]])
        verify(page).apply(formCaptor.capture())(any(), any())
        formCaptor.getValue.value mustBe Some(exceededDate)
      }

    }

    "update registration and redirect on " when {
      "groups enabled" in {
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val result =
          controller.submit()(
            postJsonRequestEncoded(("exceeded-threshold-weight-date.day", "1"),
                                   ("exceeded-threshold-weight-date.month", "4"),
                                   ("exceeded-threshold-weight-date.year", "2022")
            )
          )

        status(result) mustBe SEE_OTHER

        modifiedRegistration.liabilityDetails.dateExceededThresholdWeight mustBe Some(
          Date(LocalDate.of(2022, 4, 1))
        )
        redirectLocation(result) mustBe Some(routes.RegistrationTypeController.displayPage().url)
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user submits invalid exceed threshold weight date" in {
        authorizedUser()
        val result =
          controller.submit()(postRequest(Json.toJson(Date(LocalDate.of(2012, 5, 1)))))

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
          controller.submit()(
            postJsonRequestEncoded(("exceeded-threshold-weight-date.day", "1"),
                                   ("exceeded-threshold-weight-date.month", "4"),
                                   ("exceeded-threshold-weight-date.year", "2022")
            )
          )

        intercept[DownstreamServiceError](status(result))
      }

      "user submits form and a registration update runtime exception occurs" in {
        authorizedUser()
        mockRegistrationException()
        val result =
          controller.submit()(
            postJsonRequestEncoded(("exceeded-threshold-weight-date.day", "1"),
                                   ("exceeded-threshold-weight-date.month", "4"),
                                   ("exceeded-threshold-weight-date.year", "2022")
            )
          )

        intercept[RuntimeException](status(result))
      }
    }
  }
}
