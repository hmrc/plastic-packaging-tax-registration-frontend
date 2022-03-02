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
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExpectToExceedThresholdWeightDate
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.expect_to_exceed_threshold_weight_date_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

@ViewTest
class ExpectToExceedThresholdWeightDateViewSpec extends UnitViewSpec with Matchers {

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private val expectToExceedThresholdWeightDate =
    new ExpectToExceedThresholdWeightDate(mockAppConfig, fakeClock)

  private val page = inject[expect_to_exceed_threshold_weight_date_page]

  private def createView(form: Form[Date] = expectToExceedThresholdWeightDate.form()): Document =
    page(form)(journeyRequest, messages)

  "Expect to Exceed Threshold Weight Date View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    // TODO: fix test when previous page is available to link to
    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(
        routes.ExpectToExceedThresholdWeightDateController.displayPage()
      )
    }

    "display title" in {
      view.select("title").text() must include(
        messages("liability.expectToExceedThreshold.date.question")
      )
    }

    "display section header" in {
      view.select("span#section-header").text() must include(messages("liability.sectionHeader"))
    }

    "display question" in {
      view.select("h1").text() must include(
        messages("liability.expectToExceedThreshold.date.question")
      )
    }

    "display question hint" in {
      view.getElementById("expect-to-exceed-threshold-weight-date-hint") must containMessage(
        "liability.expectToExceedThreshold.date.hint"
      )
    }

    "display day input box" in {
      view.getElementsByAttributeValueMatching("for", "day").text() must include(
        messages("date.day")
      )
    }

    "display month input box" in {
      view.getElementsByAttributeValueMatching("for", "month").text() must include(
        messages("date.month")
      )
    }

    "display year input box" in {
      view.getElementsByAttributeValueMatching("for", "year").text() must include(
        messages("date.year")
      )
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "display data in date inputs" in {
      val form = expectToExceedThresholdWeightDate.form()
        .fill(Date(Some(1), Some(5), Some(2022)))
      val view = createView(form)

      view.getElementById("day").attr("value") mustBe "1"
      view.getElementById("month").attr("value") mustBe "5"
      view.getElementById("year").attr("value") mustBe "2022"
    }

    "display error" when {
      "no date entered" in {
        val form = expectToExceedThresholdWeightDate.form().fillAndValidate(Date(None, None, None))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary

        view must haveGovukFieldError("expect-to-exceed-threshold-weight-date", "Enter the day")
        view must haveGovukFieldError("expect-to-exceed-threshold-weight-date", "Enter the month")
        view must haveGovukFieldError("expect-to-exceed-threshold-weight-date", "Enter the year")
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(expectToExceedThresholdWeightDate.form())(journeyRequest, messages)
    page.render(expectToExceedThresholdWeightDate.form(), journeyRequest, messages)
  }

}
