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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import views.html.liability.tax_start_date_page

import java.time.LocalDate

class TaxStartDateViewSpec extends UnitViewSpec {

  private val page             = inject[tax_start_date_page]
  private val aDate: LocalDate = LocalDate.of(2022, 4, 14)

  "liability tax start date page" when {

    val view: Document = page(aDate, isDateFromBackwardsTest = true)

    "have the correct Title" in {
      view.select("title").first must containMessage("liability.taxStartDate.title")
    }

    "have a section header" in {
      view.getElementById("section-header").text() mustBe messages("liability.sectionHeader")
    }

    "have a page heading" in {
      view.getElementsByClass("govuk-heading-l").text() mustBe messages("liability.taxStartDate.pageHeading")
    }

    "have show the start date" in {
      view.getElementsByClass("govuk-body").get(0).text() mustBe messages("liability.taxStartDate.hint", "14 April 2022")
    }

    "explain how the date was arrived at" when {
      "showing the date from the backwards test" in {
        page(aDate, isDateFromBackwardsTest = true)
          .getElementsByClass("govuk-body")
          .get(1)
          .text() mustBe messages("liability.taxStartDate.threshHoldBreached.hint")
      }

      "showing the date from the forwards test" in {
        page(aDate, isDateFromBackwardsTest = false)
          .getElementsByClass("govuk-body")
          .get(1)
          .text() mustBe messages("liability.taxStartDate.realisedThresholdWouldBeExceeded.hint")
      }
    }

    "display 'Continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(aDate, true)(registrationJourneyRequest, messages)
    page.render(aDate, isDateFromBackwardsTest = true, registrationJourneyRequest, messages)
  }

}
