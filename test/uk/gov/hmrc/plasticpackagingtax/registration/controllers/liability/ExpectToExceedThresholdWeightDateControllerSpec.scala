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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExpectToExceedThresholdWeightDate
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.expect_to_exceed_threshold_weight_date_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone
import scala.concurrent.Future

class ExpectToExceedThresholdWeightDateControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private val expectToExceedThresholdWeightDate =
    new ExpectToExceedThresholdWeightDate(mockAppConfig, fakeClock)

  private val mockPage = mock[expect_to_exceed_threshold_weight_date_page]

  private val aFreshRegistration = Registration("123")

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPage)
    mockRegistrationFind(aFreshRegistration)
    when(mockPage.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("Expect to exceed threshold date")
    )
    authorizedUser()
  }

  val controller = new ExpectToExceedThresholdWeightDateController(
    mockAuthAction,
    mockJourneyAction,
    mockRegistrationConnector,
    mcc,
    expectToExceedThresholdWeightDate,
    mockPage
  )

  "Expect to Exceed Threshold Weight Controller" should {
    "display page with empty form" when {
      "no date previously captured" in {
        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "Expect to exceed threshold date"
      }
    }
    "display page with populated form" when {
      "date previously captured" in {
        val dateRealised = Date(LocalDate.of(2022, 4, 1))
        mockRegistrationFind(
          aFreshRegistration.copy(liabilityDetails =
            LiabilityDetails(exceededThresholdWeight = Some(false),
                             expectToExceedThresholdWeight = Some(true),
                             dateRealisedExpectedToExceedThresholdWeight = Some(dateRealised)
            )
          )
        )
        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "Expect to exceed threshold date"

        val formCaptor: ArgumentCaptor[Form[Date]] = ArgumentCaptor.forClass(classOf[Form[Date]])
        verify(mockPage).apply(formCaptor.capture())(any(), any())
        formCaptor.getValue.value mustBe Some(dateRealised)
      }

    }

    "redisplay page when invalid date supplied" in {
      val resp = controller.submit()(postRequestEncoded(Date(LocalDate.of(2022, 7, 1))))

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe "Expect to exceed threshold date"
    }

    "update registration and redirect when valid date supplied" in {
      mockRegistrationUpdate()
      val dateRealised = Date(LocalDate.of(2022, 4, 1))
      val resp = await(
        controller.submit()(
          postJsonRequestEncoded(
            ("expect-to-exceed-threshold-weight-date.day",
             dateRealised.date.getDayOfMonth.toString
            ),
            ("expect-to-exceed-threshold-weight-date.month",
             dateRealised.date.getMonthValue.toString
            ),
            ("expect-to-exceed-threshold-weight-date.year", dateRealised.date.getYear.toString)
          )
        )
      )

      redirectLocation(Future.successful(resp)) mustBe Some(
        routes.TaxStartDateController.displayPage().url
      )
      modifiedRegistration.liabilityDetails.dateRealisedExpectedToExceedThresholdWeight mustBe Some(
        dateRealised
      )
    }

    "unauthorised access throws exceptions" when {
      "displaying page" in {
        unAuthorizedUser()
        intercept[RuntimeException] {
          await(controller.displayPage()(getRequest()))
        }
      }
      "submitting date" in {
        unAuthorizedUser()
        intercept[RuntimeException] {
          await(controller.submit()(postRequestEncoded(Date(LocalDate.of(2022, 2, 1)))))
        }
      }
    }
  }
}
