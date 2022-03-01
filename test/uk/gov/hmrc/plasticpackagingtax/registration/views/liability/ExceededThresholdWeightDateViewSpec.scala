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

package uk.gov.hmrc.plasticpackagingtax.registration.views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExceededThresholdWeightDate
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.exceeded_threshold_weight_date_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

@ViewTest
class ExceededThresholdWeightDateViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[exceeded_threshold_weight_date_page]

  private val mockAppConfig =
    mock[AppConfig]

  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private def createView(
    form: Form[Date] = new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply(),
    backLink: Call = liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage()
  ): Document =
    page(form, backLink)(journeyRequest, messages)

  "Exceeded Threshold Weight Date View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(
        messages("liability.exceededThresholdWeightDate.title")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("liability.exceededThresholdWeightDate.sectionHeader")
      )
    }

    "display hint" in {

      view.getElementById("exceeded-threshold-weight-date-hint") must containMessage(
        "liability.exceededThresholdWeightDate.hint"
      )
    }

    "display day input box" in {

      view.getElementsByAttributeValueMatching("for",
                                               "exceeded-threshold-weight-date.day"
      ).text() must include(messages("date.day"))
    }

    "display month input box" in {

      view.getElementsByAttributeValueMatching("for",
                                               "exceeded-threshold-weight-date.month"
      ).text() must include(messages("date.month"))
    }

    "display year input box" in {

      view.getElementsByAttributeValueMatching("for",
                                               "exceeded-threshold-weight-date.year"
      ).text() must include(messages("date.year"))
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Exceeded threshold weight date view when filled" should {

    "display data in date inputs" in {
      val registration = aRegistration(
        withLiabilityDetails(
          LiabilityDetails(dateExceededThresholdWeight = Some(Date(LocalDate.of(2022, 4, 1))))
        )
      )
      val form = new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply()
        .fill(registration.liabilityDetails.dateExceededThresholdWeight.get)
      val view = createView(form)

      view.getElementById("exceeded-threshold-weight-date.day").attr("value") mustBe "1"
      view.getElementById("exceeded-threshold-weight-date.month").attr("value") mustBe "4"
      view.getElementById("exceeded-threshold-weight-date.year").attr("value") mustBe "2022"
    }
  }

  "display error" when {

    "Exceeded threshold weight date is not valid" in {

      val form = new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply()
        .fillAndValidate(Date(LocalDate.of(2012, 4, 1)))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("exceeded-threshold-weight-date",
                                    "Liability date must be today or in the past"
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply(),
           liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage()
    )(journeyRequest, messages)
    page.render(new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply(),
                liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage(),
                journeyRequest,
                messages
    )
  }

}
