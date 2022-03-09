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
import play.api.data.{Form, FormError}
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
    form: Form[Date] = new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply()
  ): Document =
    page(form)(journeyRequest, messages)

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
        liabilityRoutes.ExceededThresholdWeightController.displayPage()
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

    "date is invalid" in {
      val exceededForm = form("", "", "")
      exceededForm.errors.size mustBe 3
      validateErrors(exceededForm.errors,
                     Seq(
                       FormError("exceeded-threshold-weight-date.day",
                                 List("liability.exceededThresholdWeightDate.empty.error"),
                                 List()
                       )
                     )
      )
    }

    "day is invalid" in {
      val exceededForm = form("dfd", "4", "2022")
      exceededForm.errors.size mustBe 1
      validateErrors(exceededForm.errors,
                     Seq(
                       FormError("exceeded-threshold-weight-date",
                                 List("liability.exceededThresholdWeightDate.formatting.error"),
                                 List()
                       )
                     )
      )
    }
    "day is empty" in {
      val exceededForm = form("", "4", "2022")
      exceededForm.errors.size mustBe 2
      validateErrors(
        exceededForm.errors,
        Seq(FormError("day", List("liability.exceededThresholdWeightDate.one.field"), List("day")))
      )
    }
    "month is invalid" in {
      val exceededForm = form("1", "sd", "2022")
      exceededForm.errors.size mustBe 1
      validateErrors(exceededForm.errors.toList,
                     List(
                       FormError("exceeded-threshold-weight-date",
                                 List("liability.exceededThresholdWeightDate.formatting.error"),
                                 List()
                       )
                     )
      )
    }
    "month is empty" in {
      val exceededForm = form("1", "", "2022")
      exceededForm.errors.size mustBe 2
      validateErrors(
        exceededForm.errors,
        Seq(
          FormError("month", List("liability.exceededThresholdWeightDate.one.field"), List("month"))
        )
      )
    }
    "year is invalid" in {
      val exceededForm = form("1", "4", "xfd")
      exceededForm.errors.size mustBe 1
      validateErrors(exceededForm.errors,
                     Seq(
                       FormError("exceeded-threshold-weight-date",
                                 List("liability.exceededThresholdWeightDate.formatting.error"),
                                 List()
                       )
                     )
      )
    }
    "year is empty" in {
      val exceededForm = form("1", "4", "")
      exceededForm.errors.size mustBe 2
      validateErrors(
        exceededForm.errors,
        Seq(
          FormError("year", List("liability.exceededThresholdWeightDate.one.field"), List("year"))
        )
      )
    }
    "month and year is empty" in {
      val exceededForm = form("1", "", "")
      exceededForm.errors.size mustBe 3
      validateErrors(exceededForm.errors,
                     Seq(
                       FormError("month and year",
                                 List("liability.exceededThresholdWeightDate.two.required.fields"),
                                 List("month", "year")
                       )
                     )
      )
    }
    "day and year is empty" in {
      val exceededForm = form("", "4", "")
      exceededForm.errors.size mustBe 3
      validateErrors(exceededForm.errors,
                     Seq(
                       FormError("day and year",
                                 List("liability.exceededThresholdWeightDate.two.required.fields"),
                                 List("day", "year")
                       )
                     )
      )
    }
    "day and month is empty" in {
      val exceededForm = form("", "", "2022")
      exceededForm.errors.size mustBe 3
      validateErrors(exceededForm.errors,
                     Seq(
                       FormError("day and month",
                                 List("liability.exceededThresholdWeightDate.two.required.fields"),
                                 List("day", "month")
                       )
                     )
      )
    }
  }

  private def form(day: String, month: String, year: String): Form[Date] =
    new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply().bind(toMap(day, month, year))

  private def validateErrors(actual: Seq[FormError], expected: Seq[FormError]): Unit =
    expected.head mustBe actual.head

  private def toMap(day: String, month: String, year: String): Map[String, String] =
    Map("exceeded-threshold-weight-date.day"   -> day,
        "exceeded-threshold-weight-date.month" -> month,
        "exceeded-threshold-weight-date.year"  -> year
    )

  override def exerciseGeneratedRenderingMethods() = {
    page.f(new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply())(journeyRequest,
                                                                              messages
    )
    page.render(new ExceededThresholdWeightDate(mockAppConfig, fakeClock).apply(),
                journeyRequest,
                messages
    )
  }

}
