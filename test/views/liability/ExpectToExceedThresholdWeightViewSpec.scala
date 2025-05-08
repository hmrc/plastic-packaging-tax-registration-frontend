/*
 * Copyright 2025 HM Revenue & Customs
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
import forms.liability.ExpectToExceedThresholdWeight
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import views.html.liability.expect_to_exceed_threshold_weight_page

class ExpectToExceedThresholdWeightViewSpec extends UnitViewSpec with Matchers {
  private val page: expect_to_exceed_threshold_weight_page = inject[expect_to_exceed_threshold_weight_page]
  private val form: Form[Boolean]                          = new ExpectToExceedThresholdWeight().apply()

  private def createView(form: Form[Boolean] = form): Document =
    page(form)(registrationJourneyRequest, messages)

  "ExceededThresholdWeight View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("liability.expectToExceedThresholdWeight.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(messages("liability.sectionHeader"))
      view.getElementsByClass("govuk-heading-l").text() must include(
        messages("liability.expectToExceedThresholdWeight.title")
      )
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "display paragraph contents" in {
      view.getElementsByClass("govuk-body").get(0).text() mustBe messages("liability.expectToExceedThresholdWeight.p1")
      view.getElementsByClass("govuk-body").get(1).text() mustBe messages(
        "liability.expectToExceedThresholdWeight.inset.p1"
      )
      view.getElementsByClass("dashed-list-item").get(0).text() mustBe messages(
        "liability.expectToExceedThresholdWeight.inset.bullet.1"
      )
      view.getElementsByClass("dashed-list-item").get(1).text() mustBe messages(
        "liability.expectToExceedThresholdWeight.inset.bullet.2"
      )
      view.getElementsByClass("govuk-body").get(4).text() mustBe messages(
        "liability.expectToExceedThresholdWeight.example.1"
      )
      view.getElementsByClass("govuk-body").get(5).text() mustBe messages(
        "liability.expectToExceedThresholdWeight.example.2"
      )
      val text = messages("liability.expectToExceedThresholdWeight.p3.link")

      view.getElementsByClass("govuk-body").get(6).text() mustBe messages(
        "liability.expectToExceedThresholdWeight.p3",
        text
      )
    }

    "display radio buttons" in {
      view.getElementsByClass("govuk-fieldset__legend--m").text() mustBe messages(
        "liability.expectToExceedThresholdWeight.question"
      )
      view.getElementById("value-hint").text() mustBe messages("liability.expectToExceedThresholdWeight.hint")
      view.getElementsByClass("govuk-radios__item").get(0).text() mustBe "Yes"
      view.getElementsByClass("govuk-radios__item").get(1).text() mustBe "No"
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(form)(registrationJourneyRequest, messages)
    page.render(form, registrationJourneyRequest, messages)
  }

}
