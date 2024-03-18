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

package views.liability

import base.unit.UnitViewSpec
import config.AppConfig
import forms.liability.ExceededThresholdWeightDate
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import views.html.liability.exceeded_threshold_weight_date_page

import java.time.LocalDate

class ExceededThresholdWeightDateViewSpec extends UnitViewSpec with Matchers {
  private val page: exceeded_threshold_weight_date_page = inject[exceeded_threshold_weight_date_page]
  private val appConfig: AppConfig                      = inject[AppConfig]
  private val form: Form[LocalDate]                     = new ExceededThresholdWeightDate(appConfig).apply()

  private def createView(form: Form[LocalDate] = form): Document =
    page(form)(registrationJourneyRequest, messages)

  "ExceededThresholdWeightDateView" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("liability.exceededThresholdWeightDate.title"))
    }

    "display header" in {
      view.getElementsByClass("govuk-caption-l").text() must include(messages("liability.sectionHeader"))
      view.getElementsByClass("govuk-heading-l").text() must include(
        messages("liability.exceededThresholdWeightDate.title")
      )
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "display paragraph contents" in {
      view.getElementsByClass("govuk-body").get(0).text() mustBe messages("liability.exceededThresholdWeightDate.line1")
      view.getElementsByClass("govuk-body").get(1).text() mustBe messages("liability.exceededThresholdWeightDate.line2")
    }

    "display date fields" in {
      view.getElementsByClass("govuk-fieldset__legend--m").text() mustBe messages(
        "liability.exceededThresholdWeightDate.question"
      )
      view.getElementById("exceeded-threshold-weight-date-hint").text() mustBe messages(
        "liability.exceededThresholdWeightDate.hint"
      )
      view.getElementsByClass("govuk-date-input__label").get(0).text() mustBe "Day"
      view.getElementsByClass("govuk-date-input__label").get(1).text() mustBe "Month"
      view.getElementsByClass("govuk-date-input__label").get(2).text() mustBe "Year"
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(form)(registrationJourneyRequest, messages)
    page.render(form, registrationJourneyRequest, messages)
  }

}
